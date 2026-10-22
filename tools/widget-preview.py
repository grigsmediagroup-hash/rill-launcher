from pathlib import Path
import subprocess, xml.etree.ElementTree as ET, re, time
ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[str(ADB),'-P','5041','-s','127.0.0.1:5561']
def run(*args):
    p=subprocess.run(PREFIX+list(args),capture_output=True,timeout=30)
    if p.returncode:raise RuntimeError(p.stderr.decode(errors='replace'))
    return p.stdout
def ui():
    run('shell','uiautomator','dump','/sdcard/rill-widget.xml')
    return ET.fromstring(run('shell','cat','/sdcard/rill-widget.xml'))
def tap(label):
    for n in ui().iter('node'):
        if n.get('text')==label:
            a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
            run('shell','input','tap',str((a+c)//2),str((b+d)//2));return
    raise AssertionError(label)
subprocess.run([str(ADB),'-P','5041','connect','127.0.0.1:5561'],check=True,capture_output=True)
assert run('shell','getprop','ro.kernel.qemu').strip()==b'1'
run('shell','am','start','-n','dev.personal.rill/.HomeActivity')
tap('⋯');tap('Add home widget');tap('Calendar month view')
tree=ui();(ROOT/'build/widget-binding-ui.xml').write_bytes(ET.tostring(tree))
print([n.get('text') for n in tree.iter('node') if n.get('text')],flush=True)
