"""Integration checks and actual screenshots, restricted to the preview emulator."""
from pathlib import Path
import subprocess, re, time, xml.etree.ElementTree as ET, json

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = '127.0.0.1:5561'
PREFIX = [str(ADB), '-P', '5041', '-s', SERIAL]
RESULTS = []

def adb(*args, binary=False):
    r = subprocess.run(PREFIX + list(args), capture_output=True, timeout=40)
    if r.returncode: raise RuntimeError(r.stderr.decode(errors='replace'))
    return r.stdout if binary else r.stdout.decode(errors='replace').strip()

def ui():
    adb('shell', 'uiautomator', 'dump', '/sdcard/rill-test.xml')
    raw = adb('shell', 'cat', '/sdcard/rill-test.xml')
    return ET.fromstring(raw)

def node(label, tree=None):
    tree = tree if tree is not None else ui()
    for n in tree.iter('node'):
        if n.get('text') == label: return n
    raise AssertionError('Missing UI item: ' + label)

def center(n):
    x1,y1,x2,y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    return (x1+x2)//2, (y1+y2)//2

def tap(label):
    x,y = center(node(label)); adb('shell','input','tap',str(x),str(y))

def back(): adb('shell','input','keyevent','4')
def home():
    adb('shell','am','start','-n','dev.personal.rill/.HomeActivity')
    time.sleep(.4)
def screenshot(name):
    ui()  # Wait for a settled UI, including the sheet entrance.
    (ROOT / 'preview' / (name+'.png')).write_bytes(adb('exec-out','screencap','-p',binary=True))
def passed(label):
    RESULTS.append(label); print('PASS: '+label, flush=True)

subprocess.run([str(ADB),'-P','5041','connect',SERIAL],check=True,capture_output=True)
assert adb('shell','getprop','ro.kernel.qemu') == '1', 'Tests require emulator'
adb('install','-r',str(ROOT/'dist/Rill-0.1.0.apk'))
adb('shell','am','force-stop','dev.personal.rill')
adb('shell','logcat','-c')
home()
tap('⋯')
while True:
    tap('Manage home widgets');tree=ui()
    names=[n.get('text','') for n in tree.iter('node') if re.match(r'^\d+ · ',n.get('text',''))]
    if not names:back();break
    tap(names[0]);tap('Remove')
back();home();node('Phone');passed('Cold start and persisted favorites')
screenshot('home')
tap('All apps  ↗'); tree=ui()
assert any(n.get('text','').startswith('ALL APPS') for n in tree.iter('node'))
passed('All-apps navigation'); screenshot('all-apps')
x,y=center(next(n for n in ui().iter('node') if n.get('content-desc')=='Search apps')); adb('shell','input','tap',str(x),str(y)); adb('shell','input','text','cam'); tree=ui(); node('Camera',tree)
assert any('RESULTS' in n.get('text','') for n in tree.iter('node'))
passed('Search filters installed apps'); screenshot('search')
back(); back(); home()
x,y=center(node('Phone')); adb('shell','input','swipe',str(x-60),str(y),str(x+240),str(y),'180')
tree=ui();node('Open app',tree)
assert not any(n.get('text')=='Remove favorite' for n in tree.iter('node'))
passed('Swipe pop-up cancels click and long press'); screenshot('popup'); back()
x,y=center(node('Phone'));adb('shell','input','swipe',str(x),str(y),str(x),str(y),'800')
node('Remove favorite');passed('Long press editing');back()
tap('⋯');node('Choose default home app');passed('Settings sheet');screenshot('settings')
tap('Second clock / time zone');node('Find a city or region')
tree=ui()
for n in tree.iter('node'):
    if n.get('class')=='android.widget.EditText':
        x,y=center(n);adb('shell','input','tap',str(x),str(y));break
adb('shell','input','text','London');node('Europe/London');tap('Europe/London');node('Phone')
passed('Second time zone selection and activity recreation');screenshot('dual-clock')
tap('⋯');tap('Theme');tap('Azure');node('Phone');screenshot('azure');passed('Azure theme recreation')
tap('⋯');tap('Theme');tap('Slate');node('Phone');screenshot('slate');passed('Slate theme recreation')
tap('⋯');tap('Theme');tap('Plum');node('Phone');screenshot('plum');passed('Plum theme recreation')
tap('⋯');tap('Theme');tap('Deep water');node('Phone')
tap('⋯');tap('Add home widget');tree=ui()
widget_names=[n.get('text') for n in tree.iter('node') if n.get('class')=='android.widget.TextView' and n.get('text')]
(ROOT/'build/widget-picker-items.json').write_text(json.dumps(widget_names,indent=2))
back();passed('Widget picker cancellation');back();home()
adb('shell','input','tap','680','813');tree=ui()
assert any(n.get('text','').startswith('ALL APPS') for n in tree.iter('node'))
passed('Alphabet jumps into all-apps list');screenshot('alphabet-list');home()
adb('shell','dumpsys','gfxinfo','dev.personal.rill','reset')
for _ in range(8):
    adb('shell','input','swipe','680','200','680','1000','250')
home()
(ROOT/'build/emulator-gfxinfo.txt').write_text(adb('shell','dumpsys','gfxinfo','dev.personal.rill'))
(ROOT/'build/emulator-memory.txt').write_text(adb('shell','dumpsys','meminfo','dev.personal.rill'))
logs=adb('shell','logcat','-d','-t','1500','-s','AndroidRuntime')
(ROOT/'build/emulator-runtime.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs, logs
passed('No fatal exception during tested flows')
(ROOT/'build/smoke-results.json').write_text(json.dumps(RESULTS,indent=2))
print('Finished: '+str(len(RESULTS))+' checks',flush=True)
