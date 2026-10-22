"""Capture real current-build settings on the isolated emulator; never a physical device."""
from pathlib import Path
import subprocess,time,re,xml.etree.ElementTree as ET,json,os
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.environ.get('ANDROID_SDK_ROOT',str(Path.home()/'AppData/Local/Android/Sdk')))/'platform-tools/adb.exe'
PREFIX=[str(ADB),'-P','5041','-s','127.0.0.1:5561']
OUT=ROOT/'docs/images/current';OUT.mkdir(parents=True,exist_ok=True)
shots=[]
def adb(*args):
 r=subprocess.run(PREFIX+list(args),capture_output=True,timeout=40)
 if r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
 return r.stdout
for _ in range(30):
 if adb('shell','getprop','sys.boot_completed').strip()==b'1':break
 time.sleep(2)
else:raise RuntimeError('Emulator boot timeout')
assert adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
adb('install','-r',str(ROOT/'dist/Rill-0.1.0.apk'))
adb('shell','wm','size','720x1280');adb('shell','wm','density','320')
adb('shell','input','keyevent','82')
def ui():
 adb('shell','uiautomator','dump','/sdcard/rill-gallery.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/rill-gallery.xml'))
def find(label,tree=None):
 for n in (tree if tree is not None else ui()).iter('node'):
  if n.get('text')==label or n.get('content-desc')==label:return n
 return None
def tap(label,scroll=False):
 for i in range(7 if scroll else 1):
  n=find(label)
  if n is not None:
   x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
   if y2>y1 and y1>=0:
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3);return
  if scroll:adb('shell','input','swipe','350','1000','350','450','250')
 raise AssertionError('Missing control: '+label)
def back():adb('shell','input','keyevent','4');time.sleep(.3)
def home():
 adb('shell','am','start','-W','-n','dev.personal.rill/.HomeActivity');time.sleep(.6)
def capture(name):
 ui();(OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'));shots.append(name);print('CAPTURE '+name,flush=True)
def settings():home();tap('⋯')
home();capture('home')
settings();capture('settings-top')
adb('shell','input','swipe','350','1000','350','450','300');capture('settings-middle')
adb('shell','input','swipe','350','1000','350','450','300');capture('settings-bottom')
settings();tap('Theme',True);capture('theme-picker');back()
for label,name in [('AMOLED black','amoled'),('Transparent wallpaper','transparent'),('Device wallpaper (dimmed)','dimmed'),('Deep water','deep-water'),('Azure','azure'),('Slate','slate'),('Plum','plum')]:
 settings();tap('Theme',True);tap(label);time.sleep(.8);home();capture(name)
settings();tap('Theme',True);tap('Deep water');home()
for label,name in [('Wallpaper dimming','wallpaper-dimming'),('Text size','text-size'),('Home vertical position','home-position'),('Second clock / time zone','timezone-picker'),('Gesture guide','gesture-guide'),('Manage home widgets','manage-widgets'),('Add home widget','widget-picker')]:
 settings();tap(label,True);capture(name);back()
home();tap('All apps  ↗');capture('all-apps')
tap('Search apps');adb('shell','input','text','cam');capture('search');back();home()
adb('shell','input','swipe','90','490','360','490','180');capture('app-popup');home()
settings();tap('Black-and-white app icons',True);home();capture('colored-icons')
settings();tap('Black-and-white app icons',True);home();capture('monochrome-icons')
(ROOT/'build/gallery-captures.json').write_text(json.dumps(shots,indent=2))
print('COMPLETE '+str(len(shots))+' screenshots',flush=True)
