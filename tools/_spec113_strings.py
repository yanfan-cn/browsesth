from pathlib import Path

ROOT = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\res")

replacements = {
    "values/strings.xml": [
        (
            '<string name="add_to_bookshelf">Add to Reading list</string>',
            '<string name="add_to_bookshelf">Join list</string>',
        ),
    ],
    "values-zh-rCN/strings.xml": [
        (
            '<string name="add_to_bookshelf">放入阅读列表</string>',
            '<string name="add_to_bookshelf">加入列表</string>',
        ),
        (
            '<string name="check_add_bookshelf">是否将《%s》放入阅读列表？</string>',
            '<string name="check_add_bookshelf">是否将《%s》加入列表？</string>',
        ),
    ],
    "values-zh-rTW/strings.xml": [
        (
            '<string name="add_to_bookshelf">放入閱讀列表</string>',
            '<string name="add_to_bookshelf">加入列表</string>',
        ),
    ],
    "values-zh-rHK/strings.xml": [
        (
            '<string name="add_to_bookshelf">放入閱讀列表</string>',
            '<string name="add_to_bookshelf">加入列表</string>',
        ),
    ],
}

favorite_insert = {
    "values/strings.xml": (
        '    <string name="browser_add_favorite">Add to Favorites</string>\n'
        '    <string name="browser_favorite_added">Added to Favorites</string>\n'
    ),
    "values-zh-rCN/strings.xml": (
        '    <string name="browser_add_favorite">加入收藏</string>\n'
        '    <string name="browser_favorite_added">已加入收藏</string>\n'
    ),
    "values-zh-rTW/strings.xml": (
        '    <string name="browser_add_favorite">加入收藏</string>\n'
        '    <string name="browser_favorite_added">已加入收藏</string>\n'
    ),
    "values-zh-rHK/strings.xml": (
        '    <string name="browser_add_favorite">加入收藏</string>\n'
        '    <string name="browser_favorite_added">已加入收藏</string>\n'
    ),
}

for rel, reps in replacements.items():
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    for old, new in reps:
        if old not in text:
            print("MISS", rel, old)
        else:
            text = text.replace(old, new)
            print("OK", rel)
    if "browser_add_favorite" not in text:
        marker = '<string name="bookmark_added">'
        idx = text.find(marker)
        if idx < 0:
            print("NO MARKER", rel)
        else:
            text = text[:idx] + favorite_insert[rel] + text[idx:]
            print("INSERT", rel)
    path.write_text(text, encoding="utf-8")

print("done")
