from pathlib import Path
p = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\haleydu\cimoc\model\ImageUrl.java")
t = p.read_text(encoding="utf-8")
t = t.replace("@Override\n        public String[] convertToEntityProperty", "public String[] convertToEntityProperty")
t = t.replace("@Override\n        public String convertToDatabaseValue", "public String convertToDatabaseValue")
p.write_bytes(t.encode("utf-8"))
print("ok")
