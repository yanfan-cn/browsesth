import re, sys
path = sys.argv[1]
min_y = int(sys.argv[2]) if len(sys.argv) > 2 else 0
xml = open(path, encoding="utf-8", errors="ignore").read()
for m in re.finditer(r"<node[^>]+>", xml):
    s = m.group(0)
    bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
    if not bm:
        continue
    y1 = int(bm.group(2))
    if y1 < min_y:
        continue
    if 'clickable="true"' not in s:
        continue
    cd = re.search(r'content-desc="([^"]*)"', s)
    tx = re.search(r'text="([^"]*)"', s)
    x1, y1, x2, y2 = map(int, bm.groups())
    print("%d,%d bounds=[%d,%d][%d,%d] cd=%s text=%s" % (
        (x1+x2)//2, (y1+y2)//2, x1, y1, x2, y2,
        cd.group(1) if cd else "", tx.group(1) if tx else ""))