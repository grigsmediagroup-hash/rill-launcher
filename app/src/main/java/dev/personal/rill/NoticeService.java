package dev.personal.rill;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.Intent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Notifications never leave the device and are never persisted. */
public class NoticeService extends NotificationListenerService {
    public static final String CHANGED = "dev.personal.rill.NOTICES_CHANGED";
    private static final LinkedHashMap<String, StatusBarNotification> active = new LinkedHashMap<>();
    private static NoticeService instance;
    public static synchronized List<StatusBarNotification> forPackage(String pkg) {
        List<StatusBarNotification> out = new ArrayList<>();
        for (StatusBarNotification s : active.values()) if (s.getPackageName().equals(pkg)) out.add(s);
        return out;
    }
    public static synchronized String preview(String pkg) {
        String text = "";
        for (StatusBarNotification s : active.values()) {
            if (!pkg.equals(s.getPackageName()) || (s.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
            CharSequence value = s.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
            if (value != null) text = value.toString();
        }
        return text;
    }
    public static void dismiss(String key) { if (instance != null) instance.cancelNotification(key); }
    @Override public void onListenerConnected() {
        instance = this;
        synchronized (NoticeService.class) {
            active.clear();
            StatusBarNotification[] list = getActiveNotifications();
            if (list != null) for (StatusBarNotification s : list) active.put(s.getKey(), s);
        }
        changed();
    }
    @Override public void onNotificationPosted(StatusBarNotification s) {
        synchronized (NoticeService.class) { active.put(s.getKey(), s); }
        changed();
    }
    @Override public void onNotificationRemoved(StatusBarNotification s) {
        synchronized (NoticeService.class) { active.remove(s.getKey()); }
        changed();
    }
    @Override public void onListenerDisconnected() {
        instance = null;
        synchronized (NoticeService.class) { active.clear(); }
        changed();
    }
    @Override public void onDestroy() { instance = null; synchronized (NoticeService.class) { active.clear(); } super.onDestroy(); }
    private void changed() { sendBroadcast(new Intent(CHANGED).setPackage(getPackageName())); }
}
