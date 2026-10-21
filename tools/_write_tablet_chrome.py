# -*- coding: utf-8 -*-
from pathlib import Path

# Minimal fix script: rewrite TabletBrowserChrome with ComposeModifier alias.
# Content loaded from sibling .kt.src if present; else abort.
src = Path(__file__).with_name('_tablet_chrome.kt.src')
out = Path(r'D:/cursor/browsesth/upstream/legado-with-MD3/app/src/main/java/io/legado/app/ui/browser/TabletBrowserChrome.kt')
if not src.is_file():
    raise SystemExit(f'missing {src}')
out.write_text(src.read_text(encoding='utf-8'), encoding='utf-8')
print('wrote', out, 'chars', out.stat().st_size)
