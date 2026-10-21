from pathlib import Path
p = Path(r"D:/cursor/browsesth/upstream/legado-with-MD3/app/src/main/java/io/legado/app/data/AppDatabase.kt")
text = p.read_text(encoding="utf-8")
needle = "IdReadFinished}, '\u5df2\u8bfb', -28, 0"
repl = "IdReadFinished}, '\u5df2\u8bfb', -28, 1"
if needle not in text:
    for i, l in enumerate(text.splitlines(), 1):
        if "IdReadFinished}," in l and "select" in l:
            print("found", i, l.encode("unicode_escape"))
    raise SystemExit("needle missing")
p.write_text(text.replace(needle, repl, 1), encoding="utf-8")
print("ok")
