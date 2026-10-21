import re, subprocess, time, os

def dump(path):
    subprocess.check_call(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"])
    subprocess.check_call(["adb", "pull", "/sdcard/ui.xml", path])

def clickables(path, min_y=0):
    xml = open(path, encoding="utf-8", errors="ignore").read()
    out = []
    for m in re.finditer(r"<node[^>]+>", xml):
        s = m.group(0)
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
        if not bm or 'clickable="true"' not in s:
            continue
        x1,y1,x2,y2 = map(int, bm.groups())
        if y1 < min_y:
            continue
        cd = re.search(r'content-desc="([^"]*)"', s)
        tx = re.search(r'text="([^"]*)"', s)
        out.append(((x1+x2)//2,(y1+y2)//2,x1,y1,x2,y2, cd.group(1) if cd else "", tx.group(1) if tx else ""))
    return out, xml

base = r"D:\cursor\browsesth"
# ensure on app
subprocess.call(["adb", "shell", "am", "start", "-n", "com.browsesth.app/io.legado.app.ui.main.MainActivity"])
time.sleep(3)
dump(os.path.join(base, "tmp-ui-a.xml"))
items, xml = clickables(os.path.join(base, "tmp-ui-a.xml"))
print("NTP clickables sample:")
for it in items[:15]:
    print(it)

# tap search (heuristic: EditText parent clickable around mid)
# From prior: 597,564
subprocess.call(["adb", "shell", "input", "tap", "597", "564"])
time.sleep(0.5)
subprocess.call(["adb", "shell", "input", "text", "https://m.baidu.com"])
subprocess.call(["adb", "shell", "input", "keyevent", "66"])
time.sleep(8)
dump(os.path.join(base, "tmp-ui-b.xml"))
items, xml = clickables(os.path.join(base, "tmp-ui-b.xml"), 0)
print("SESSION clickables:")
for it in items:
    print(it)
print("xml has EditText", "EditText" in xml)
print("cds", re.findall(r'content-desc="([^"]+)"', xml)[:50])

# find tab-like: bottom bar 4th button or content-desc containing digit-only badge
candidates = [it for it in items if it[1] > 1800]
print("BOTTOM:", candidates)

# clear log and tap last bottom clickable cluster
subprocess.call(["adb", "logcat", "-c"])
pid_before = subprocess.check_output(["adb", "shell", "pidof", "com.browsesth.app"]).decode().strip()
print("pid_before", pid_before)

# Prefer clickable whose bounds look like tab count in floating bar (before reader fab)
# Layout: bar with 4 items + fab. Tab is 4th in bar. Typical x around 650-780 y ~2150
taps = []
for it in candidates:
    taps.append((it[0], it[1], it[6], it[7]))
# also try known positions
taps.append((700, 2150, "guess", "tab"))
taps.append((650, 2200, "guess2", "tab"))
taps.append((780, 2180, "guess3", "tab"))

# Tap each unique bottom center until crash or tabs UI
seen=set()
for x,y,cd,tx in taps:
    key=(x,y)
    if key in seen: continue
    seen.add(key)
    print("TAP", x,y,cd,tx)
    subprocess.call(["adb", "shell", "input", "tap", str(x), str(y)])
    time.sleep(2)
    pid = subprocess.check_output(["adb", "shell", "pidof", "com.browsesth.app"]).decode().strip()
    print("pid_after", pid)
    if not pid or pid != pid_before:
        print("CRASH DETECTED")
        log = subprocess.check_output(["adb", "logcat", "-d", "-v", "brief"]).decode("utf-8", "ignore")
        open(os.path.join(base, "_tabs_crash_log.txt"), "w", encoding="utf-8").write(log)
        # print fatal lines
        for line in log.splitlines():
            if any(k in line for k in ("FATAL", "SIGSEGV", "AndroidRuntime", "computeTransform", "Fatal signal")):
                print(line)
        break
    dump(os.path.join(base, "tmp-ui-c.xml"))
    _, xml2 = clickables(os.path.join(base, "tmp-ui-c.xml"))
    if "常规" in xml2 or "无痕" in xml2 or "新标签页" in xml2:
        print("TABS OPENED OK at", x,y)
        break
else:
    print("No crash and tabs not detected; saving log anyway")
    log = subprocess.check_output(["adb", "logcat", "-d", "-v", "brief"]).decode("utf-8", "ignore")
    open(os.path.join(base, "_tabs_crash_log.txt"), "w", encoding="utf-8").write(log)