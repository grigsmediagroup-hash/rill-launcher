package dev.personal.rill;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.session.*;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.Collator;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

/** A small framework-only home app. No network, analytics, or dependency runtime. */
public class HomeActivity extends Activity {
    private static final int INK = Color.rgb(239,245,244), MUTED = Color.rgb(163,181,188), ACCENT = Color.rgb(167,227,203);
    private final ColorMatrixColorFilter monochromeIcons=createMonochromeFilter();
    private static ColorMatrixColorFilter createMonochromeFilter(){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(0);return new ColorMatrixColorFilter(matrix);}
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<App> apps = new ArrayList<>(), shown = new ArrayList<>();
    private final ArrayList<String> favorites = new ArrayList<>();
    private final Set<String> hidden = new HashSet<>();
    private final Set<String> iconsLoading = new HashSet<>();
    private final ArrayList<Dialog> sheets = new ArrayList<>();
    private final LinkedHashMap<String,Drawable> icons = new LinkedHashMap<String,Drawable>(64, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String,Drawable> entry) { return size() > 64; }
    };
    private SharedPreferences prefs;
    private FrameLayout root;
    private LinearLayout page, header, widgetSlot;
    private ListView list;
    private View alphabetSpace;
    private AppsAdapter adapter;
    private EditText search;
    private TextView section, letterBubble;
    private TextView battery;
    private int renderedTheme;
    private Alphabet alphabet;
    private AppWidgetHost host;
    private AppWidgetManager widgetManager;
    private boolean allApps, searchMode, registered, alive, loading;
    private int pendingWidget = -1, widgetIndex;
    private String widgetLocation = "home";
    private Dialog popup;
    private LinearLayout popupWidgets;
    private String popupKey;
    private final HashMap<Integer,AppWidgetHostView> widgetViews = new HashMap<>();
    private float downX, downY;
    private boolean globalSwipe;

    static class App {
        final String key, pkg, original;
        final ComponentName component;
        String label, normalized;
        App(ResolveInfo info, PackageManager pm) {
            component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            key = component.flattenToString(); pkg = info.activityInfo.packageName;
            original = info.loadLabel(pm).toString(); label = original;
        }
    }
    static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); wallpaperText(v); return v;
    }
    private void wallpaperText(TextView v) { v.setShadowLayer(dp(2),0,dp(1),0xe6000000); }
    private GradientDrawable panel(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView button(String label, Runnable action) {
        TextView b = text(label, 15, ACCENT); b.setGravity(Gravity.CENTER_VERTICAL); b.setPadding(dp(16),dp(12),dp(16),dp(12));
        b.setMinHeight(dp(48)); b.setBackground(new RippleDrawable(ColorStateListCompat.color(0x33a7e3cb), panel(0x00ffffff,14), null));
        b.setOnClickListener(v -> action.run()); return b;
    }
    // ColorStateList wrapper keeps call sites short without support libraries.
    static class ColorStateListCompat { static android.content.res.ColorStateList color(int c) { return android.content.res.ColorStateList.valueOf(c); } }

    @Override public void onCreate(Bundle saved) {
        int initialTheme=getSharedPreferences("rill",MODE_PRIVATE).getInt("theme",0);
        if(initialTheme==4||initialTheme==6)setTheme(R.style.WallpaperTheme);
        super.onCreate(saved); alive = true;
        prefs = getSharedPreferences("rill", MODE_PRIVATE);
        favorites.addAll(split(prefs.getString("favorites", "")));
        hidden.addAll(prefs.getStringSet("hidden", Collections.emptySet()));
        host = new AppWidgetHost(this, 710); widgetManager = AppWidgetManager.getInstance(this);
        if (saved != null) {
            pendingWidget = saved.getInt("pendingWidget", -1);
            widgetLocation = saved.getString("widgetLocation", "home");
        }
        recoverPending();
        buildUi(); loadApps();
    }
    private ArrayList<String> split(String s) {
        ArrayList<String> out = new ArrayList<>();
        if (!s.isEmpty()) Collections.addAll(out, s.split("\\|")); return out;
    }
    private void saveFavorites() { prefs.edit().putString("favorites", TextUtils.join("|", favorites)).apply(); }
    private void buildUi() {
        root = new FrameLayout(this){
            final Paint navigationShade=new Paint();
            @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);navigationShade.setShader(new LinearGradient(0,h-dp(80),0,h,Color.TRANSPARENT,0x99000000,Shader.TileMode.CLAMP));}
            @Override public void draw(Canvas canvas){if(renderedTheme==4||renderedTheme==6)canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);super.draw(canvas);}
            @Override protected void dispatchDraw(Canvas canvas){if(renderedTheme!=5)canvas.drawRect(0,Math.max(0,getHeight()-dp(80)),getWidth(),getHeight(),navigationShade);super.dispatchDraw(canvas);}
        };
        int theme = prefs.getInt("theme", 0);
        renderedTheme=theme;
        if (theme == 4||theme==6) {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            updateWallpaperDim();
        } else {
            getWindow().setFormat(PixelFormat.OPAQUE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            getWindow().setBackgroundDrawable(new ColorDrawable(0xff101c23));
            int[][] palettes = {{0xff102c32,0xff101b24,0xff121922},{0xff039ee0,0xff0561c9,0xff0a3698},{0xff4c5f70,0xff394956,0xff26333f},{0xff642242,0xff3e1a37,0xff20152b}};
            if(theme==5){getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));root.setBackgroundColor(Color.BLACK);}
            else{int[] colors = palettes[Math.max(0,Math.min(3,theme))];root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors));}
        }
        // Draw the wallpaper/theme behind the status bar, but keep all controls below its inset.
        // Extend the background behind both bars; insets keep controls away from system buttons.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS|WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(theme==5?Color.BLACK:0x66000000);
        getWindow().setNavigationBarColor(theme==5?Color.BLACK:Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(26),dp(8),dp(46),0);
        root.addView(page, new FrameLayout.LayoutParams(-1,-1));
        header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0,dp(prefs.getInt("offset",64)),0,dp(12));
        TextClock clock = new TextClock(this); clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextSize(42); clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL)); clock.setTextColor(INK);
        wallpaperText(clock);
        clock.setOnClickListener(v -> safeStart(new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)));
        clock.setOnLongClickListener(v -> { settings(); return true; }); header.addView(clock);
        TextClock date = new TextClock(this); date.setFormat12Hour("EEEE, MMM d"); date.setFormat24Hour("EEEE, MMM d");
        date.setTextColor(MUTED); date.setTextSize(13); date.setPadding(0,dp(2),0,dp(8));
        wallpaperText(date);
        date.setOnClickListener(v -> safeStart(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)));
        LinearLayout glance=new LinearLayout(this);glance.setGravity(Gravity.CENTER_VERTICAL);glance.addView(date);
        battery=text("",12,ACCENT);battery.setPadding(dp(12),dp(2),0,dp(8));battery.setVisibility(prefs.getBoolean("battery",true)?View.VISIBLE:View.GONE);
        battery.setOnClickListener(v->safeStart(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)));glance.addView(battery);header.addView(glance);
        String zone=prefs.getString("secondClock","");
        if(!zone.isEmpty()){
            LinearLayout second=new LinearLayout(this);second.setGravity(Gravity.CENTER_VERTICAL);second.setPadding(0,0,0,dp(8));
            TextView city=text(zone.substring(zone.lastIndexOf('/')+1).replace('_',' ')+"  ",13,MUTED);second.addView(city);
            TextClock abroad=new TextClock(this);abroad.setTimeZone(zone);abroad.setFormat12Hour("h:mm a");abroad.setFormat24Hour("HH:mm");abroad.setTextColor(INK);abroad.setTextSize(16);wallpaperText(abroad);second.addView(abroad);
            second.setOnClickListener(v->chooseSecondClock());header.addView(second);
        }
        widgetSlot = new LinearLayout(this); widgetSlot.setOrientation(LinearLayout.VERTICAL); header.addView(widgetSlot);
        page.addView(header);
        search = new EditText(this); search.setSingleLine(true); search.setTextSize(20); search.setTextColor(INK);
        search.setHintTextColor(MUTED); search.setHint("Find an app"); search.setPadding(dp(10),0,dp(10),0);
        search.setBackground(panel(0x25ffffff,14)); search.setVisibility(View.GONE);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        page.addView(search, new LinearLayout.LayoutParams(-1,dp(52)));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {} public void onTextChanged(CharSequence s,int start,int before,int count) { refresh(); }
            public void afterTextChanged(Editable e) {}
        });
        search.setOnEditorActionListener((v,id,event) -> { if (!shown.isEmpty()) launch(shown.get(0)); return true; });
        section = text("YOUR FAVORITES",11,MUTED); section.setPadding(dp(6),dp(12),0,dp(6)); page.addView(section);
        list = new ListView(this); list.setDivider(null); list.setCacheColorHint(Color.TRANSPARENT);
        list.setVerticalScrollBarEnabled(false); list.setClipToPadding(false); list.setPadding(0,0,0,dp(12));
        page.removeView(header);header.setLayoutParams(new AbsListView.LayoutParams(-1,-2,AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER));list.addHeaderView(header,null,false);
        alphabetSpace=new View(this);alphabetSpace.setLayoutParams(new AbsListView.LayoutParams(-1,0,AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER));list.addFooterView(alphabetSpace,null,false);
        list.setSelector(new ColorDrawable(Color.TRANSPARENT)); adapter = new AppsAdapter(); list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id) -> {int index=pos-list.getHeaderViewsCount();if(index>=0&&index<shown.size())launch(shown.get(index));});
        list.setOnItemLongClickListener((p,v,pos,id) -> {int index=pos-list.getHeaderViewsCount();if(index>=0&&index<shown.size())appMenu(shown.get(index));return true;});
        list.setOnTouchListener(new View.OnTouchListener() {
            float x,y;
            boolean horizontal;
            App swiped;
            public boolean onTouch(View v,MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { x=e.getX(); y=e.getY(); horizontal=false; swiped=null; }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !horizontal && Math.abs(e.getX()-x)>dp(76) && Math.abs(e.getY()-y)<dp(42)) {
                    int pos=list.pointToPosition((int)x,(int)y)-list.getHeaderViewsCount();
                    if(pos>=0 && pos<shown.size()) {
                        swiped=shown.get(pos); horizontal=true;
                        MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);list.onTouchEvent(cancel);cancel.recycle();
                    }
                }
                if(horizontal) {
                    if(e.getActionMasked()==MotionEvent.ACTION_UP && swiped!=null) {
                        if(e.getX()>x)showPopup(swiped);else for(StatusBarNotification n:NoticeService.forPackage(swiped.pkg))if(n.isClearable())NoticeService.dismiss(n.getKey());
                    }
                    return true;
                }
                return false;
            }
        });
        page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout dock=new LinearLayout(this); dock.setGravity(Gravity.CENTER_VERTICAL);
        TextView all=button("All apps  ↗", () -> { allApps=!allApps; searchMode=false; search.setVisibility(View.GONE); hideKeyboard(); refresh(); });
        all.setContentDescription("Toggle all apps"); dock.addView(all,new LinearLayout.LayoutParams(0,dp(56),1));
        LinearLayout searchButton=new LinearLayout(this);searchButton.setGravity(Gravity.CENTER);searchButton.setPadding(dp(12),0,dp(14),0);searchButton.setContentDescription("Search apps");searchButton.setOnClickListener(v->openSearch());
        View magnifier=new View(this){final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);protected void onDraw(Canvas c){p.setColor(INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setStrokeCap(Paint.Cap.ROUND);c.drawCircle(dp(9),dp(9),dp(6),p);c.drawLine(dp(13.5f),dp(13.5f),dp(19),dp(19),p);}};
        searchButton.addView(magnifier,new LinearLayout.LayoutParams(dp(22),dp(22)));
        searchButton.setBackground(new RippleDrawable(ColorStateListCompat.color(0x33a7e3cb),panel(0x22ffffff,28),null));
        dock.addView(searchButton,new LinearLayout.LayoutParams(dp(48),dp(48)));dock.addView(button("⋯", () -> settings())); page.addView(dock);
        alphabet = new Alphabet(); FrameLayout.LayoutParams rail=new FrameLayout.LayoutParams(dp(40),-1,Gravity.RIGHT);
        rail.topMargin=dp(50); rail.bottomMargin=dp(64); root.addView(alphabet,rail);
        letterBubble=text("A",30,0xff102c32);letterBubble.setShadowLayer(0,0,0,0);letterBubble.setGravity(Gravity.CENTER);letterBubble.setBackground(panel(ACCENT,28));
        letterBubble.setVisibility(View.GONE); FrameLayout.LayoutParams bubble=new FrameLayout.LayoutParams(dp(56),dp(56),Gravity.RIGHT|Gravity.TOP);
        bubble.rightMargin=dp(55); root.addView(letterBubble,bubble);
        setContentView(root);root.requestApplyInsets(); renderWidgets("home",widgetSlot); refresh();
    }
    private void updateWallpaperDim(){
        if(renderedTheme==6){root.setBackgroundColor(Color.TRANSPARENT);return;}
        int alpha=Math.round(255*prefs.getInt("wallpaperDim",40)/100f);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{(alpha/3)<<24,alpha<<24}));
    }
    private void loadApps() {
        if(loading)return; loading=true;
        worker.execute(() -> {
            ArrayList<App> result=new ArrayList<>(); PackageManager pm=getPackageManager();
            Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo info:pm.queryIntentActivities(intent,0)) {
                if(info.activityInfo.packageName.equals(getPackageName()))continue;
                App a=new App(info,pm); a.label=prefs.getString("name:"+a.key,a.original); a.normalized=fold(a.label); result.add(a);
            }
            Collator sorter=Collator.getInstance(); Collections.sort(result,(a,b)->sorter.compare(a.label,b.label));
            main.post(() -> {
                loading=false; if(!alive)return; apps.clear(); apps.addAll(result); icons.clear();
                if(!prefs.getBoolean("initialized",false)) {
                    String[] wanted={"phone","messag","camera","chrome","browser","music","settings"};
                    for(String name:wanted)for(App a:apps)if(a.normalized.contains(name)&&!favorites.contains(a.key)){favorites.add(a.key);break;}
                    if(favorites.isEmpty())for(int i=0;i<Math.min(5,apps.size());i++)favorites.add(apps.get(i).key);
                    prefs.edit().putBoolean("initialized",true).apply(); saveFavorites();
                }
                refresh();
            });
        });
    }
    private App find(String key) { for(App a:apps)if(a.key.equals(key))return a; return null; }
    private void refresh() {
        if(adapter==null)return;
        shown.clear(); String query=fold(search.getText().toString().trim());
        if(allApps || searchMode) {
            for(App a:apps)if(!hidden.contains(a.key)&&a.normalized.startsWith(query))shown.add(a);
            if(!query.isEmpty())for(App a:apps)if(!hidden.contains(a.key)&&!a.normalized.startsWith(query)&&a.normalized.contains(query))shown.add(a);
        }
        else for(String key:favorites){App a=find(key);if(a!=null&&!hidden.contains(key))shown.add(a);}
        if(allApps||searchMode){if(list.getHeaderViewsCount()>0){list.removeHeaderView(header);list.setSelection(0);}}
        else if(list.getHeaderViewsCount()==0){header.setLayoutParams(new AbsListView.LayoutParams(-1,-2,AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER));list.addHeaderView(header,null,false);list.setSelection(0);}
        section.setText(searchMode ? shown.size()+" RESULTS" : allApps ? "ALL APPS · "+shown.size() : shown.isEmpty()?"LONG PRESS AN APP TO ADD FAVORITES":"YOUR FAVORITES");
        section.setVisibility(!allApps&&!searchMode&&!shown.isEmpty()?View.GONE:View.VISIBLE);
        adapter.notifyDataSetChanged();list.invalidateViews();if(searchMode)list.setSelection(0);alphabet.setVisibility(searchMode?View.GONE:View.VISIBLE);alphabet.invalidate();
        alphabetSpace.setLayoutParams(new AbsListView.LayoutParams(-1,allApps&&!searchMode?Math.max(0,list.getHeight()-dp(prefs.getBoolean("compact",true)?52:62)-list.getPaddingTop()-list.getPaddingBottom()):0,AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER));
    }
    private void launch(App app) {
        hideKeyboard();
        try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)); }
        catch(RuntimeException e){toast("App unavailable. Refreshing your list.");loadApps();}
    }
    private void safeStart(Intent i) { try{startActivity(i);}catch(RuntimeException e){toast("No app is available for this action.");} }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private void hideKeyboard(){ ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0); search.clearFocus(); }
    private void openSearch(){ allApps=true; searchMode=true; search.setVisibility(View.VISIBLE); search.requestFocus(); refresh(); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT); }
    private void home(){for(Dialog dialog:new ArrayList<>(sheets))if(dialog.isShowing())dialog.dismiss();allApps=false;searchMode=false;search.setText("");search.setVisibility(View.GONE);hideKeyboard();refresh();list.setSelection(0);}
    @Override public void onBackPressed(){if(searchMode||allApps)home();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);home();}
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();globalSwipe=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !globalSwipe && Math.abs(e.getX()-downX)<dp(45) && Math.abs(e.getY()-downY)>dp(120)){
            int[] pos=new int[2]; header.getLocationOnScreen(pos);
            boolean blankHeader=!allApps&&!searchMode && downY<pos[1]+header.getHeight() && widgetSlot.getChildCount()==0;
            boolean dockStart=downY>root.getHeight()-dp(75);
            if(blankHeader||dockStart){
                globalSwipe=true; MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();
                if(e.getY()<downY)openSearch();else notificationShade();return true;
            }
        }
        if(globalSwipe)return true;return super.dispatchTouchEvent(e);
    }
    private void notificationShade(){
        try{Object status=getSystemService("statusbar");Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(status);}
        catch(Exception e){toast("Pull down from the status bar to open notifications.");}
    }
    class AppsAdapter extends BaseAdapter {
        public int getCount(){return shown.size();}public Object getItem(int p){return shown.get(p);}public long getItemId(int p){return shown.get(p).key.hashCode();}
        public View getView(int position,View recycled,android.view.ViewGroup parent){
            LinearLayout row; ImageView icon; TextView title,preview;
            if(recycled==null||!(recycled.getTag() instanceof Object[])){
                row=new LinearLayout(HomeActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(4),dp(4),dp(4));
                icon=new ImageView(HomeActivity.this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
                LinearLayout labels=new LinearLayout(HomeActivity.this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(18),0,0,0);
                title=text("",20,INK);title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);labels.addView(title);
                preview=text("",12,MUTED);preview.setMaxLines(1);preview.setEllipsize(TextUtils.TruncateAt.END);labels.addView(preview);
                row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.setTag(new Object[]{icon,title,preview});
            }else row=(LinearLayout)recycled;
            Object[] holder=(Object[])row.getTag();icon=(ImageView)holder[0];title=(TextView)holder[1];preview=(TextView)holder[2];
            App a=shown.get(position);title.setText(a.label);title.setTextSize(prefs.getInt("fontSize",18));row.setMinimumHeight(dp(prefs.getBoolean("compact",true)?52:62));
            String notice=prefs.getBoolean("previews",true)?NoticeService.preview(a.pkg):"";preview.setText(notice);preview.setVisibility(notice.isEmpty()?View.GONE:View.VISIBLE);
            icon.setVisibility(prefs.getBoolean("icons",true)?View.VISIBLE:View.GONE);icon.setImageDrawable(icons.get(a.key));
            if(prefs.getBoolean("monochromeIcons",true))icon.setColorFilter(monochromeIcons);else icon.clearColorFilter();
            icon.setTag(a.key);
            if(prefs.getBoolean("icons",true)&&!icons.containsKey(a.key)&&iconsLoading.add(a.key)){
                final ImageView target=icon;
                worker.execute(() -> {
                    Drawable drawable;
                    try{drawable=getPackageManager().getActivityIcon(a.component);}catch(PackageManager.NameNotFoundException e){main.post(()->iconsLoading.remove(a.key));return;}
                    main.post(() -> { iconsLoading.remove(a.key);if(!alive)return; icons.put(a.key,drawable);adapter.notifyDataSetChanged(); });
                });
            }
            row.setContentDescription(a.label+(notice.isEmpty()?"":", "+notice));return row;
        }
    }
    class Alphabet extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); final String letters="★ABCDEFGHIJKLMNOPQRSTUVWXYZ#"; int selected=-1;
        Alphabet(){super(HomeActivity.this);setContentDescription("Alphabet navigation. Slide to a letter. Star opens favorites.");setFocusable(true);}
        @Override protected void onDraw(Canvas c){
            float step=getHeight()/(float)letters.length();paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",0));
            for(int i=0;i<letters.length();i++){
                float y=(i+.5f)*step;float distance=Math.abs(i-selected);float wave=selected<0?0:Math.max(0,1-distance/4f);
                paint.setTextSize(dp(10+7*wave));paint.setColor(i==selected?ACCENT:0xff8ba2ac);
                paint.setShadowLayer(dp(2),0,dp(1),0xe6000000);
                c.drawText(letters.substring(i,i+1),getWidth()/2f-dp(15)*wave,y-(paint.ascent()+paint.descent())/2,paint);
            }
        }
        private void choose(float y){
            letterBubble.setY(Math.max(dp(8),Math.min(root.getHeight()-dp(64),getTop()+y-dp(28))));
            int index=Math.max(0,Math.min(letters.length()-1,(int)(y/getHeight()*letters.length())));if(index==selected)return;selected=index;
            if(index==0){home();}else{
                if(!allApps||searchMode){allApps=true;searchMode=false;search.setText("");search.setVisibility(View.GONE);hideKeyboard();refresh();}
                char letter=letters.charAt(index);int target=-1;
                for(int i=0;i<shown.size();i++){String name=shown.get(i).normalized;char first=name.isEmpty()?'#':Character.toUpperCase(name.charAt(0));
                    if((letter=='#'&&(first<'A'||first>'Z'))||(letter!='#'&&first>=letter&&first<='Z')){target=i;break;}}
                if(target>=0)list.setSelectionFromTop(target+list.getHeaderViewsCount(),0);
            }
            letterBubble.setText(letters.substring(index,index+1));letterBubble.setVisibility(View.VISIBLE);invalidate();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_MOVE){getParent().requestDisallowInterceptTouchEvent(true);choose(e.getY());return true;}
            if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){selected=-1;letterBubble.setVisibility(View.GONE);invalidate();performClick();return true;}return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
    private void appMenu(App a){
        boolean favored=favorites.contains(a.key);
        String[] options={favored?"Remove favorite":"Add favorite","Move favorite up","Move favorite down","Rename","Hide app","App info","Open pop-up"};
        new AlertDialog.Builder(this).setTitle(a.label).setItems(options,(d,which)->{
            if(which==0){if(favored)favorites.remove(a.key);else{favorites.add(a.key);hidden.remove(a.key);saveHidden();}saveFavorites();refresh();}
            if(which==1||which==2){int i=favorites.indexOf(a.key),j=i+(which==1?-1:1);if(i>=0&&j>=0&&j<favorites.size()){Collections.swap(favorites,i,j);saveFavorites();refresh();}}
            if(which==3){EditText name=new EditText(this);name.setText(a.label);new AlertDialog.Builder(this).setTitle("App name").setView(name).setPositiveButton("Save",(x,y)->{String value=name.getText().toString().trim();if(!value.isEmpty()){prefs.edit().putString("name:"+a.key,value).apply();loadApps();}}).setNeutralButton("Reset",(x,y)->{prefs.edit().remove("name:"+a.key).apply();loadApps();}).setNegativeButton("Cancel",null).show();}
            if(which==4){hidden.add(a.key);saveHidden();refresh();}
            if(which==5)safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.pkg)));
            if(which==6)showPopup(a);
        }).show();
    }
    private void saveHidden(){prefs.edit().putStringSet("hidden",new HashSet<>(hidden)).apply();}
    private LinearLayout sheet(String title){
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(20),dp(20),dp(14));content.setBackground(panel(0xff1d3039,24));
        TextView heading=text(title,24,INK);heading.setPadding(dp(8),0,0,dp(12));content.addView(heading);return content;
    }
    private Dialog showSheet(LinearLayout content){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(content);
        Dialog dialog=new Dialog(this);dialog.getWindow();dialog.setContentView(scroll);Window w=dialog.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.45f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setGravity(Gravity.BOTTOM);
        dialog.show();w.setLayout(-1,-2);sheets.add(dialog);dialog.setOnDismissListener(d->sheets.remove(dialog));return dialog;
    }
    private void showPopup(App a){
        if(popup!=null)popup.dismiss();popupKey=a.key;
        LinearLayout content=sheet(a.label);content.addView(button("Open app",()->{popup.dismiss();launch(a);}));
        for(StatusBarNotification sbn:NoticeService.forPackage(a.pkg)){
            Notification n=sbn.getNotification();if((n.flags&Notification.FLAG_GROUP_SUMMARY)!=0)continue;
            CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),body=n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if(body==null)body=n.extras.getCharSequence(Notification.EXTRA_TEXT);
            TextView notice=text((title==null?"":title+"\n")+(body==null?"":body.toString()),15,INK);notice.setPadding(dp(8),dp(12),dp(8),dp(8));content.addView(notice);
            if(n.contentIntent!=null)content.addView(button("View notification",()->{try{n.contentIntent.send();popup.dismiss();}catch(PendingIntent.CanceledException e){toast("Notification expired.");}}));
            if(sbn.isClearable())content.addView(button("Dismiss",()->{NoticeService.dismiss(sbn.getKey());popup.dismiss();}));
            if(n.actions!=null)for(Notification.Action action:n.actions){
                if(action.actionIntent==null)continue;
                RemoteInput[] inputs=action.getRemoteInputs();
                if(inputs!=null&&inputs.length>0&&Build.VERSION.SDK_INT>=24)content.addView(button(action.title.toString(),()->reply(action,inputs)));
                else if(inputs==null)content.addView(button(action.title.toString(),()->{try{action.actionIntent.send();}catch(PendingIntent.CanceledException e){toast("Action expired.");}}));
            }
        }
        addShortcuts(a,content);addMedia(a,content);
        for(String key:split(prefs.getString("links:"+a.key,""))){App linked=find(key);if(linked!=null)content.addView(button(linked.label,()->{popup.dismiss();launch(linked);}));}
        popupWidgets=new LinearLayout(this);popupWidgets.setOrientation(LinearLayout.VERTICAL);content.addView(popupWidgets);renderWidgets(a.key,popupWidgets);
        content.addView(button("Edit related apps",()->editLinks(a)));
        content.addView(button("Add widget",()->pickWidget(a.key)));
        content.addView(button("Manage widgets",()->manageWidgets(a.key)));
        popup=showSheet(content);popup.setOnDismissListener(d->{sheets.remove(d);detachWidgets(popupWidgets);popupWidgets=null;popupKey=null;});
        if(!prefs.getBoolean("reduceMotion",false)){content.setTranslationY(dp(22));content.setAlpha(0);content.animate().translationY(0).alpha(1).setDuration(160).start();}
    }
    private void reply(Notification.Action action,RemoteInput[] inputs){
        EditText input=new EditText(this);input.setHint("Reply");new AlertDialog.Builder(this).setTitle("Reply").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Send",(d,w)->{
            String value=input.getText().toString().trim();if(value.isEmpty())return;Bundle result=new Bundle();for(RemoteInput r:inputs)if(r.getAllowFreeFormInput())result.putCharSequence(r.getResultKey(),value);
            Intent fill=new Intent();RemoteInput.addResultsToIntent(inputs,fill,result);
            try{action.actionIntent.send(this,0,fill);toast("Reply sent to the app.");}catch(PendingIntent.CanceledException e){toast("Reply expired. Open the app to reply.");}
        }).show();
    }
    private void addShortcuts(App a,LinearLayout content){
        if(Build.VERSION.SDK_INT<25)return;
        try{
            LauncherApps launcher=(LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);if(!launcher.hasShortcutHostPermission())return;
            LauncherApps.ShortcutQuery query=new LauncherApps.ShortcutQuery().setPackage(a.pkg).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC|LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
            List<ShortcutInfo> shortcuts=launcher.getShortcuts(query,android.os.Process.myUserHandle());
            if(shortcuts!=null)for(ShortcutInfo shortcut:shortcuts)if(shortcut.isEnabled())content.addView(button(String.valueOf(shortcut.getShortLabel()),()->{
                try{launcher.startShortcut(shortcut.getPackage(),shortcut.getId(),null,null,android.os.Process.myUserHandle());popup.dismiss();}catch(RuntimeException e){toast("Shortcut unavailable.");}
            }));
        }catch(SecurityException e){/* Default home role required. */}
    }
    private void addMedia(App a,LinearLayout content){
        try{
            MediaSessionManager manager=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
            for(android.media.session.MediaController controller:manager.getActiveSessions(new ComponentName(this,NoticeService.class)))if(controller.getPackageName().equals(a.pkg)){
                MediaMetadata meta=controller.getMetadata();if(meta!=null){String title=meta.getString(MediaMetadata.METADATA_KEY_TITLE);if(title!=null)content.addView(text(title,16,INK));}
                LinearLayout controls=new LinearLayout(this);
                controls.addView(button("Previous",()->controller.getTransportControls().skipToPrevious()));
                PlaybackState state=controller.getPlaybackState();boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
                controls.addView(button(playing?"Pause":"Play",()->{if(playing)controller.getTransportControls().pause();else controller.getTransportControls().play();}));
                controls.addView(button("Next",()->controller.getTransportControls().skipToNext()));content.addView(controls);break;
            }
        }catch(SecurityException e){/* Notification access is optional. */}
    }
    private void editLinks(App app){
        ArrayList<String> selected=split(prefs.getString("links:"+app.key,""));String[] labels=new String[apps.size()];boolean[] checked=new boolean[apps.size()];
        for(int i=0;i<apps.size();i++){labels[i]=apps.get(i).label;checked[i]=selected.contains(apps.get(i).key);}
        new AlertDialog.Builder(this).setTitle("Related apps").setMultiChoiceItems(labels,checked,(d,i,on)->{String key=apps.get(i).key;if(on&&!selected.contains(key))selected.add(key);else if(!on)selected.remove(key);})
            .setPositiveButton("Save",(d,w)->{prefs.edit().putString("links:"+app.key,TextUtils.join("|",selected)).apply();showPopup(app);}).setNegativeButton("Cancel",null).show();
    }
    private void settings(){
        LinearLayout content=sheet("Make room for your day");
        content.addView(text("Rill · personal preview 0.1\nAndroid 5+ · offline · no ads",14,MUTED));
        content.addView(button("Choose default home app",()->safeStart(new Intent(Settings.ACTION_HOME_SETTINGS))));
        content.addView(button("Add home widget",()->pickWidget("home")));
        content.addView(button("Manage home widgets",()->manageWidgets("home")));
        content.addView(button("Theme",()->new AlertDialog.Builder(this).setTitle("Theme").setSingleChoiceItems(new String[]{"Deep water","Azure","Slate","Plum","Device wallpaper (dimmed)","AMOLED black","Transparent wallpaper"},prefs.getInt("theme",0),(d,w)->{prefs.edit().putInt("theme",w).apply();d.dismiss();recreate();}).show()));
        content.addView(button("Choose wallpaper",()->{if(prefs.getInt("theme",0)!=6)prefs.edit().putInt("theme",4).apply();safeStart(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),"Choose wallpaper"));}));
        content.addView(button("Wallpaper dimming",()->slider("Wallpaper dimming (%)",0,80,prefs.getInt("wallpaperDim",40),value->{prefs.edit().putInt("wallpaperDim",value).apply();if(renderedTheme==4)updateWallpaperDim();})));
        content.addView(button("Second clock / time zone",()->chooseSecondClock()));
        toggle(content,"Battery percentage","battery",true);
        toggle(content,"Show app icons","icons",true);toggle(content,"Compact app rows","compact",true);toggle(content,"Notification previews","previews",true);toggle(content,"Reduce motion","reduceMotion",false);
        toggle(content,"Black-and-white app icons","monochromeIcons",true);
        content.addView(button("Enable notification access",()->safeStart(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        content.addView(button("Text size",()->slider("Text size",16,28,prefs.getInt("fontSize",18),value->{prefs.edit().putInt("fontSize",value).apply();refresh();})));
        content.addView(button("Home vertical position",()->slider("Space above clock",0,160,prefs.getInt("offset",64),value->{prefs.edit().putInt("offset",value).apply();header.setPadding(0,dp(value),0,dp(12));})));
        content.addView(button("Hidden apps ("+hidden.size()+")",()->{
            ArrayList<App> values=new ArrayList<>();for(App a:apps)if(hidden.contains(a.key))values.add(a);String[] names=new String[values.size()];for(int i=0;i<names.length;i++)names[i]=values.get(i).label;
            new AlertDialog.Builder(this).setTitle("Tap to unhide").setItems(names,(d,w)->{hidden.remove(values.get(w).key);saveHidden();refresh();}).setNegativeButton("Close",null).show();
        }));
        content.addView(button("Refresh apps",()->loadApps()));
        content.addView(button("Gesture guide",()->new AlertDialog.Builder(this).setTitle("A calmer home").setMessage("Tap an app to launch. Long press to edit favorites, rename, or hide. Swipe right on an app for its pop-up; swipe left to dismiss its notifications. Slide the alphabet to jump to a letter; ★ returns to favorites. Swipe up from the bottom controls or empty clock area to search; swipe down there for notifications. Use Next / Previous to change widgets, keeping widget touch gestures intact. Home and Back return to favorites. Notification access is optional; previews stay on your phone. Android 6 has no standard app shortcut or direct-reply API, so those appear only on newer systems.").setPositiveButton("Got it",null).show()));
        showSheet(content);
    }
    private void toggle(LinearLayout container,String label,String key,boolean fallback){
        Switch s=new Switch(this);s.setText(label);s.setTextColor(INK);s.setTextSize(15);s.setPadding(dp(16),dp(10),dp(16),dp(10));s.setMinHeight(dp(48));s.setChecked(prefs.getBoolean(key,fallback));
        s.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(key,on).apply();if(key.equals("battery"))battery.setVisibility(on?View.VISIBLE:View.GONE);refresh();});container.addView(s);
    }
    private void chooseSecondClock(){
        LinearLayout content=sheet("A second time zone");EditText filter=new EditText(this);filter.setHint("Find a city or region");filter.setSingleLine();filter.setTextColor(INK);filter.setHintTextColor(MUTED);content.addView(filter);
        ArrayList<String> zones=new ArrayList<>(Arrays.asList(TimeZone.getAvailableIDs()));Collections.sort(zones);
        ArrayAdapter<String> choices=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>(zones));
        ListView values=new ListView(this);values.setAdapter(choices);content.addView(values,new LinearLayout.LayoutParams(-1,dp(260)));
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){ArrayList<String> matching=new ArrayList<>();String query=s.toString().toLowerCase(Locale.ROOT);for(String zone:zones)if(zone.toLowerCase(Locale.ROOT).contains(query))matching.add(zone);choices.clear();choices.addAll(matching);}});
        Dialog dialog=showSheet(content);values.setOnItemClickListener((p,v,pos,id)->{prefs.edit().putString("secondClock",choices.getItem(pos)).apply();dialog.dismiss();recreate();});
        content.addView(button("Turn second clock off",()->{prefs.edit().remove("secondClock").apply();dialog.dismiss();recreate();}));
    }
    interface IntChoice{void accept(int value);}
    private void slider(String title,int low,int high,int initial,IntChoice choice){
        SeekBar seek=new SeekBar(this);seek.setMax(high-low);seek.setProgress(initial-low);
        new AlertDialog.Builder(this).setTitle(title).setView(seek).setPositiveButton("Save",(d,w)->choice.accept(low+seek.getProgress())).setNegativeButton("Cancel",null).show();
    }
    private ArrayList<Integer> widgetIds(String location){
        ArrayList<Integer> ids=new ArrayList<>();for(String s:split(prefs.getString("widgets:"+location,"")))try{ids.add(Integer.parseInt(s));}catch(NumberFormatException ignored){}return ids;
    }
    private void saveWidgets(String location,List<Integer> ids){prefs.edit().putString("widgets:"+location,TextUtils.join("|",ids)).apply();}
    private void detachWidgets(LinearLayout slot){if(slot==null)return;for(AppWidgetHostView view:widgetViews.values())if(view.getParent()==slot)slot.removeView(view);}
    private void renderWidgets(String location,LinearLayout slot){
        detachWidgets(slot);slot.removeAllViews();ArrayList<Integer> ids=widgetIds(location);
        if(ids.isEmpty())return;
        int index=prefs.getInt("widgetIndex:"+location,0)%ids.size();if(index<0)index=0;int id=ids.get(index);
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);
        if(info==null){slot.addView(button("Widget unavailable · manage",()->manageWidgets(location)));return;}
        AppWidgetHostView view=widgetViews.get(id);if(view==null){view=host.createView(this,id,info);widgetViews.put(id,view);}
        if(view.getParent()!=null)((android.view.ViewGroup)view.getParent()).removeView(view);
        int width=Math.max(1,(int)((getResources().getDisplayMetrics().widthPixels-dp(80))/getResources().getDisplayMetrics().density));
        int minHeight=(int)Math.ceil(info.minHeight/getResources().getDisplayMetrics().density);
        int height=prefs.contains("height:"+id)?prefs.getInt("height:"+id,160):Math.max(minHeight,160);
        slot.addView(view,new LinearLayout.LayoutParams(-1,dp(height)));view.updateAppWidgetSize(null,width,height,width,height);
        if(ids.size()>1){final int current=index;LinearLayout nav=new LinearLayout(this);
            nav.addView(button("‹",()->{prefs.edit().putInt("widgetIndex:"+location,(current+ids.size()-1)%ids.size()).apply();renderWidgets(location,slot);}));
            TextView count=text((index+1)+" / "+ids.size(),12,MUTED);count.setGravity(Gravity.CENTER);nav.addView(count,new LinearLayout.LayoutParams(0,dp(48),1));
            nav.addView(button("›",()->{prefs.edit().putInt("widgetIndex:"+location,(current+1)%ids.size()).apply();renderWidgets(location,slot);}));slot.addView(nav);
        }
    }
    private void pickWidget(String location){
        List<AppWidgetProviderInfo> providers=widgetManager.getInstalledProviders();Collections.sort(providers,(a,b)->a.loadLabel(getPackageManager()).compareToIgnoreCase(b.loadLabel(getPackageManager())));
        if(providers.isEmpty()){toast("Install an app with a widget first.");return;}
        String[] names=new String[providers.size()];for(int i=0;i<names.length;i++)names[i]=providers.get(i).loadLabel(getPackageManager());
        new AlertDialog.Builder(this).setTitle("Choose widget").setItems(names,(d,w)->{
            if(pendingWidget!=-1){toast("Finish adding the current widget first.");return;}
            widgetLocation=location;pendingWidget=host.allocateAppWidgetId();prefs.edit().putInt("pendingId",pendingWidget).putString("pendingLocation",location).apply();
            AppWidgetProviderInfo info=providers.get(w);
            try{
                if(widgetManager.bindAppWidgetIdIfAllowed(pendingWidget,info.provider))configureWidget();
                else startActivityForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidget).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider),41);
            }catch(RuntimeException e){cancelWidget();toast("This widget could not be added.");}
        }).setNegativeButton("Cancel",null).show();
    }
    private void configureWidget(){
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(pendingWidget);if(info==null){cancelWidget();return;}
        if(info.configure!=null){try{host.startAppWidgetConfigureActivityForResult(this,pendingWidget,0,42,null);}catch(RuntimeException e){cancelWidget();toast("Widget configuration unavailable.");}}
        else finishWidget();
    }
    private void finishWidget(){
        if(pendingWidget<0)return;ArrayList<Integer> ids=widgetIds(widgetLocation);if(!ids.contains(pendingWidget))ids.add(pendingWidget);saveWidgets(widgetLocation,ids);
        prefs.edit().putInt("widgetIndex:"+widgetLocation,ids.size()-1).remove("pendingId").remove("pendingLocation").apply();pendingWidget=-1;refreshWidgetSlots();
    }
    private void cancelWidget(){if(pendingWidget>=0)host.deleteAppWidgetId(pendingWidget);pendingWidget=-1;prefs.edit().remove("pendingId").remove("pendingLocation").apply();}
    private void recoverPending(){
        if(pendingWidget!=-1)return;int stale=prefs.getInt("pendingId",-1);
        if(stale!=-1){host.deleteAppWidgetId(stale);prefs.edit().remove("pendingId").remove("pendingLocation").apply();}
    }
    private void refreshWidgetSlots(){renderWidgets("home",widgetSlot);if(popupWidgets!=null&&popupKey!=null)renderWidgets(popupKey,popupWidgets);}
    private void manageWidgets(String location){
        ArrayList<Integer> ids=widgetIds(location);String[] names=new String[ids.size()];
        for(int i=0;i<ids.size();i++){AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(ids.get(i));names[i]=(i+1)+" · "+(info==null?"Unavailable widget":info.loadLabel(getPackageManager()));}
        new AlertDialog.Builder(this).setTitle("Select widget to edit").setItems(names,(d,w)->{
            int id=ids.get(w);new AlertDialog.Builder(this).setTitle(names[w]).setItems(new String[]{"Resize height","Move earlier","Remove"},(x,action)->{
                if(action==0)slider("Widget height (dp)",60,420,prefs.getInt("height:"+id,160),value->{prefs.edit().putInt("height:"+id,value).apply();refreshWidgetSlots();});
                if(action==1&&w>0){Collections.swap(ids,w,w-1);saveWidgets(location,ids);refreshWidgetSlots();}
                if(action==2){host.deleteAppWidgetId(id);AppWidgetHostView view=widgetViews.remove(id);if(view!=null&&view.getParent()!=null)((android.view.ViewGroup)view.getParent()).removeView(view);ids.remove((Integer)id);saveWidgets(location,ids);prefs.edit().remove("height:"+id).apply();refreshWidgetSlots();}
            }).show();
        }).setNegativeButton("Close",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==41){if(result==RESULT_OK)configureWidget();else cancelWidget();}if(request==42){if(result==RESULT_OK)finishWidget();else cancelWidget();}}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("pendingWidget",pendingWidget);out.putString("widgetLocation",widgetLocation);}
    private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(Intent.ACTION_BATTERY_CHANGED.equals(i.getAction())){int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,100),status=i.getIntExtra(BatteryManager.EXTRA_STATUS,0);if(level>=0&&scale>0){String value=Math.round(100f*level/scale)+"%"+(status==BatteryManager.BATTERY_STATUS_CHARGING?" · charging":"");battery.setText(value);battery.setContentDescription("Battery "+value);}}else if(NoticeService.CHANGED.equals(i.getAction()))refresh();else loadApps();}};
    @Override protected void onStart(){super.onStart();host.startListening();IntentFilter notices=new IntentFilter(NoticeService.CHANGED);notices.addAction(Intent.ACTION_BATTERY_CHANGED);registerReceiver(receiver,notices);IntentFilter packages=new IntentFilter();packages.addAction(Intent.ACTION_PACKAGE_ADDED);packages.addAction(Intent.ACTION_PACKAGE_REMOVED);packages.addAction(Intent.ACTION_PACKAGE_CHANGED);packages.addDataScheme("package");registerReceiver(receiver,packages);registered=true;refreshWidgetSlots();refresh();loadApps();}
    @Override protected void onResume(){super.onResume();if(prefs.getInt("theme",0)!=renderedTheme)recreate();}
    @Override protected void onStop(){if(registered){unregisterReceiver(receiver);registered=false;}host.stopListening();super.onStop();}
    @Override protected void onDestroy(){alive=false;worker.shutdownNow();main.removeCallbacksAndMessages(null);for(Dialog sheet:new ArrayList<>(sheets))if(sheet.isShowing())sheet.dismiss();super.onDestroy();}
}
