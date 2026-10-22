"""Lay out original product cards around unmodified real app screenshots."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/branding';SHOTS=ROOT/'docs/images/showcase';CURRENT=ROOT/'docs/images/current'
W=1440;BG='#0c171d';INK='#eff5f4';MINT='#a7e3cb';MUTED='#a3b5bc'
def font(size,bold=False):
 for path in [Path('C:/Windows/Fonts')/('segoeuib.ttf' if bold else 'segoeui.ttf'),Path('/usr/share/fonts/truetype/dejavu')/('DejaVuSans-Bold.ttf' if bold else 'DejaVuSans.ttf')]:
  if path.exists():return ImageFont.truetype(str(path),size)
 return ImageFont.load_default()
def base(h):
 im=Image.new('RGB',(W,h),BG);d=ImageDraw.Draw(im)
 d.rounded_rectangle((20,20,W-20,h-20),radius=36,outline='#254044',width=2)
 return im,d
def text(d,xy,s,size=30,color=INK,bold=False):d.text(xy,s,font=font(size,bold),fill=color)
def phone(im,path,x,y,w):
 shot=Image.open(path).convert('RGB');h=round(w*shot.height/shot.width)
 d=ImageDraw.Draw(im);d.rounded_rectangle((x-12,y-12,x+w+12,y+h+12),radius=31,fill='#050a0e',outline='#486168',width=2)
 # Preserve the full captured screen, including its status and navigation bars.
 im.paste(shot.resize((w,h),Image.Resampling.LANCZOS),(x,y))
 return h
def pill(d,x,y,label,width):
 d.rounded_rectangle((x,y,x+width,y+50),radius=25,fill='#1b3437',outline='#385d56');text(d,(x+19,y+10),label,21,MINT,True)
im,d=base(940)
text(d,(90,81),'RILL / ANDROID LAUNCHER',23,MINT,True)
text(d,(85,191),'A little launcher.',69,bold=True)
text(d,(85,279),'A lot more room.',69,bold=True)
text(d,(90,409),'Your wallpaper. Your favorite apps.',31)
text(d,(90,457),'Everything else, a gesture away.',31,MUTED)
pill(d,90,562,'44.46 KiB APK',195);pill(d,300,562,'Android 5+',177)
text(d,(90,677),'Niagara Launcher inspired.',27,MINT)
text(d,(90,719),'Independent. Open source. Offline.',27,MUTED)
text(d,(90,836),'REAL APP CAPTURE  /  COLOR ICONS ENABLED',19,MUTED,True)
phone(im,SHOTS/'home-color.png',925,76,438)
im.save(OUT/'showcase-hero.png',optimize=True)
im,d=base(1100)
text(d,(75,68),'LESS HUNTING. MORE OPENING.',38,bold=True)
text(d,(75,128),'Browse by letter, or type a few characters.',27,MUTED)
phone(im,SHOTS/'drawer-color.png',140,241,390)
phone(im,SHOTS/'search-color.png',905,241,390)
text(d,(616,405),'A → Z',45,MINT,True)
text(d,(600,492),'Find it.',32,bold=True)
text(d,(600,539),'Open it.',32,bold=True)
text(d,(140,1000),'THE APP LIST',23,MINT,True)
text(d,(905,1000),'PREFIX-FIRST SEARCH',23,MINT,True)
im.save(OUT/'showcase-navigation.png',optimize=True)
im,d=base(1100)
text(d,(75,66),'MAKE IT FEEL LIKE YOUR PHONE.',38,bold=True)
text(d,(75,125),'Color, pure black, or the wallpaper you love.',27,MUTED)
phone(im,CURRENT/'amoled.png',85,232,345)
phone(im,SHOTS/'home-color.png',548,232,345)
phone(im,SHOTS/'settings.png',1011,232,345)
text(d,(85,910),'AMOLED BLACK',24,MINT,True)
text(d,(548,910),'YOUR WALLPAPER',24,MINT,True)
text(d,(1011,910),'YOUR SETTINGS',24,MINT,True)
text(d,(85,995),'Dual clock · Battery · Widgets · Text size · Color or grayscale icons',27,MUTED)
im.save(OUT/'showcase-customization.png',optimize=True)
print('Rendered three showcase panels from real screenshots.')
