from pathlib import Path
import subprocess,time
ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PREFIX=[str(ADB),'-P','5041','-s','127.0.0.1:5561']
def run(*args):return subprocess.run(PREFIX+list(args),capture_output=True,check=True,timeout=35)
subprocess.run([str(ADB),'-P','5041','connect','127.0.0.1:5561'],capture_output=True,check=True)
assert run('shell','getprop','ro.kernel.qemu').stdout.strip()==b'1'
run('shell','am','start','-n','dev.personal.rill/.HomeActivity')
record=subprocess.Popen(PREFIX+['shell','screenrecord','--size','720x1280','--bit-rate','2000000','--time-limit','24','/sdcard/rill-walkthrough.mp4'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
time.sleep(2)
run('shell','input','tap','200','1175');time.sleep(1)
run('shell','input','swipe','680','200','680','880','2500');time.sleep(1)
run('shell','input','swipe','680','900','680','200','1800');time.sleep(1)
run('shell','am','start','-n','dev.personal.rill/.HomeActivity');time.sleep(1)
run('shell','input','swipe','220','490','530','490','200');time.sleep(2)
run('shell','input','keyevent','4');time.sleep(1)
run('shell','input','tap','485','1175');run('shell','input','text','cam');time.sleep(2)
run('shell','input','keyevent','4');run('shell','input','keyevent','4');time.sleep(1)
out,err=record.communicate(timeout=35)
if record.returncode:raise RuntimeError(err.decode(errors='replace'))
run('pull','/sdcard/rill-walkthrough.mp4',str(ROOT/'preview/walkthrough.mp4'))
print('Recorded actual app walkthrough',flush=True)
