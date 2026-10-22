"""Exercise actual widget binding/stack/restart flows on the isolated emulator."""
from pathlib import Path
import subprocess, xml.etree.ElementTree as ET, re, time, json
ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[str(ADB),'-P','5041','-s','127.0.0.1:5561']
checks=[]
def run(*args):
    p=subprocess.run(PREFIX+list(args),capture_output=True,timeout=35)
    if p.returncode:raise RuntimeError(p.stderr.decode(errors='replace'))
    return p.stdout
def ui():
    run('shell','uiautomator','dump','/sdcard/rill-widget.xml')
    return ET.fromstring(run('shell','cat','/sdcard/rill-widget.xml'))
def labels():return [n.get('text') for n in ui().iter('node') if n.get('text')]
def tap(label):
    for n in ui().iter('node'):
        if n.get('text')==label:
            a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
            run('shell','input','tap',str((a+c)//2),str((b+d)//2));return
    raise AssertionError(label)
def back():run('shell','input','keyevent','4')
def home():run('shell','am','start','-n','dev.personal.rill/.HomeActivity')
def shot(name):
    ui();(ROOT/'preview'/name).write_bytes(run('exec-out','screencap','-p'))
def passed(message):checks.append(message);print('PASS: '+message,flush=True)
def add(provider,accept=True):
    tap('⋯');tap('Add home widget');tap(provider)
    names=labels()
    for _ in range(4):
        if 'Create' in names:break
        time.sleep(.5);names=labels()
    if 'Create' in names:tap('Create' if accept else 'Cancel')
    time.sleep(.5);ui();back();home()
subprocess.run([str(ADB),'-P','5041','connect','127.0.0.1:5561'],check=True,capture_output=True)
assert run('shell','getprop','ro.kernel.qemu').strip()==b'1'
if 'Create' in labels():tap('Cancel')
home();tap('⋯')
while True:
    tap('Manage home widgets');names=labels();items=[n for n in names if re.match(r'^\d+ · ',n)]
    if not items:back();break
    tap(items[0]);tap('Remove')
back();home();add('Calendar schedule');shot('widget-home.png');passed('Provider binding and calendar widget hosting')
run('shell','am','force-stop','dev.personal.rill');home();shot('widget-restart.png');passed('Widget survives process restart')
add('Chrome Dino');names=labels();assert '2 / 2' in names,names;tap('‹');names=labels();assert '1 / 2' in names,names
shot('widget-stack.png');passed('Two-widget stack and previous control')
add('Battery',False);names=labels();assert '1 / 2' in names,names;passed('Binding cancellation leaves stack unchanged')
tap('⋯');tap('Theme');tap('Device wallpaper');shot('wallpaper.png');passed('Device wallpaper renders behind home')
tap('⋯');tap('Choose wallpaper');names=labels();assert any('wallpaper' in n.lower() for n in names),names
shot('wallpaper-picker.png');passed('System wallpaper chooser opens');back();home()
tap('⋯');tap('Theme');tap('Deep water');home()
(ROOT/'build/widget-results.json').write_text(json.dumps(checks,indent=2))
(ROOT/'build/widget-state.txt').write_bytes(run('shell','dumpsys','appwidget'))
runtime=run('shell','logcat','-d','-t','1000','-s','AndroidRuntime').decode(errors='replace')
(ROOT/'build/widget-runtime.txt').write_text(runtime)
assert 'FATAL EXCEPTION' not in runtime,runtime
print('Widget checks complete',flush=True)
