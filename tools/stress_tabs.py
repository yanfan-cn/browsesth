import subprocess, time, re, os
base = r"D:\cursor\browsesth"

def sh(*a):
    return subprocess.check_output(list(a)).decode("utf-8","ignore").strip()

def call(*a):
    subprocess.call(list(a))

def dump(path):
    call("adb","shell","uiautomator","dump","/sdcard/ui.xml")
    call("adb","pull","/sdcard/ui.xml", path)

def has_tabs(path):
    xml=open(path,encoding="utf-8",errors="ignore").read()
    return ("常规" in xml) or ("无痕" in xml) or ("新标签页" in xml)

def pid():
    return sh("adb","shell","pidof","com.browsesth.app")

# ensure session with page
call("adb","shell","am","start","-n","com.browsesth.app/io.legado.app.ui.main.MainActivity")
time.sleep(2)
# if on tabs, back
dump(os.path.join(base,"tmp-stress.xml"))
if has_tabs(os.path.join(base,"tmp-stress.xml")):
    call("adb","shell","input","tap","90","180")
    time.sleep(1)

# open apple for squash + crash stress
call("adb","shell","input","tap","540","177")  # address bar middle
time.sleep(0.4)
call("adb","shell","input","keyevent","KEYCODE_MOVE_END")
# select all + replace
call("adb","shell","input","keyevent","KEYCODE_CTRL_A")
call("adb","shell","input","text","https://www.apple.com.cn")
call("adb","shell","input","keyevent","66")
time.sleep(8)

call("adb","logcat","-c")
p0 = pid()
print("start pid", p0)
crashed=False
for i in range(8):
    # tap tab button (4th in bar ~704,2280) or bar area
    print("round", i, "open tabs")
    call("adb","shell","input","tap","704","2280")
    time.sleep(1.5)
    p = pid()
    if not p or p != p0:
        print("CRASH on open round", i, "pid", p)
        crashed=True
        break
    dump(os.path.join(base,"tmp-stress.xml"))
    opened = has_tabs(os.path.join(base,"tmp-stress.xml"))
    print(" opened", opened)
    # back
    call("adb","shell","input","tap","90","180")
    time.sleep(1.2)
    p = pid()
    if not p or p != p0:
        print("CRASH on back round", i, "pid", p)
        crashed=True
        break
    # also try NTP path: go home somehow - skip

log = sh("adb","logcat","-d","-v","brief")
open(os.path.join(base,"_tabs_stress_log.txt"),"w",encoding="utf-8").write(log)
if crashed:
    for line in log.splitlines():
        if any(k in line for k in ("FATAL","SIGSEGV","Fatal signal","AndroidRuntime","computeTransform")):
            print(line)
else:
    print("NO CRASH in 8 open/back cycles")
    # still print any fatal if present
    fats=[ln for ln in log.splitlines() if "Fatal signal" in ln or "AndroidRuntime" in ln]
    print("fatals", len(fats))
    for ln in fats[-10:]:
        print(ln)