from pathlib import Path
import re

SRC = Path(r"D:\cursor\Cimoc\app\src\main\java\com\haleydu\cimoc\model")
DST = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\haleydu\cimoc\model")
GREENDAO_IMPORT = re.compile(r"^import org\.greenrobot\.greendao\.[^\n]+\n", re.M)
ANNOTATIONS = re.compile(
    r"@[ \t]*(Entity|Id|NotNull|Unique|Transient|Generated|Convert)(\([^)]*\))?[ \t]*\n"
)

for name in ["Chapter.java", "Comic.java", "ImageUrl.java", "Source.java", "MiniComic.java"]:
    text = (SRC / name).read_text(encoding="utf-8")
    text = GREENDAO_IMPORT.sub("", text)
    text = ANNOTATIONS.sub("", text)
    text = text.replace("implements PropertyConverter<String[], String>", "")
    text = text.replace("@NotNull ", "")
    (DST / name).write_bytes(text.encode("utf-8"))
    print("rewrote", name, (DST / name).read_bytes()[:3])

root = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\haleydu\cimoc")
for p in root.rglob("*.java"):
    b = p.read_bytes()[:4]
    if b.startswith(b"\xff\xfe") or b.startswith(b"\xfe\xff") or b.startswith(b"\xef\xbb\xbf"):
        print("BOM", p)
