#!/usr/bin/env python3
"""Vendor Cimoc parser stack into browsesth legado app (SPEC-016)."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

SRC = Path(r"D:\cursor\Cimoc\app\src\main\java\com\haleydu\cimoc")
DST = Path(
    r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\haleydu\cimoc"
)

COPY_DIRS = {
    "parser": "*",
    "soup": "*",
    "source": "*",
    "utils": {
        "include": {
            "StringUtils.java",
            "DecryptionUtils.java",
            "HttpUtils.java",
            "UicodeBackslashU.java",
            "LogUtil.java",
            "AESCrypt.java",
            "interpretationUtils.java",
            "JMTTUtil.kt",
            "CollectionUtils.java",
        }
    },
    "model": {
        "include": {
            "Chapter.java",
            "Comic.java",
            "ImageUrl.java",
            "Source.java",
            "MiniComic.java",
        }
    },
}

GREENDAO_IMPORT = re.compile(
    r"^import org\.greenrobot\.greendao\.[^\n]+\n", re.M
)
ANNOTATIONS = re.compile(
    r"@[ \t]*(Entity|Id|NotNull|Unique|Transient|Generated|Convert)(\([^)]*\))?[ \t]*\n"
)


def copy_tree() -> None:
    if DST.exists():
        shutil.rmtree(DST)
    DST.mkdir(parents=True)

    for dirname, rule in COPY_DIRS.items():
        src_dir = SRC / dirname
        dst_dir = DST / dirname
        dst_dir.mkdir(parents=True, exist_ok=True)
        for path in src_dir.iterdir():
            if not path.is_file():
                continue
            if isinstance(rule, dict):
                if path.name not in rule["include"]:
                    continue
            shutil.copy2(path, dst_dir / path.name)
            print("copied", dirname, path.name)


def strip_greendao(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    text = GREENDAO_IMPORT.sub("", text)
    text = ANNOTATIONS.sub("", text)
    # ImageUrl converter: drop PropertyConverter implements
    text = text.replace(
        "implements PropertyConverter<String[], String>",
        "",
    )
    # Chapter still implements Parcelable — keep
    path.write_text(text, encoding="utf-8")
    print("stripped", path.name)


def write_shims() -> None:
    (DST / "App.java").write_text(
        """package com.haleydu.cimoc;

import com.haleydu.cimoc.manager.PreferenceManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Minimal Cimoc App shim for browsesth SPEC-016 adapters.
 */
public final class App {
    private static OkHttpClient httpClient;
    private static final PreferenceManager PREFS = new PreferenceManager();

    private App() {}

    public static synchronized OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();
        }
        return httpClient;
    }

    public static PreferenceManager getPreferenceManager() {
        return PREFS;
    }
}
""",
        encoding="utf-8",
    )

    mgr = DST / "manager"
    mgr.mkdir(parents=True, exist_ok=True)
    (mgr / "PreferenceManager.java").write_text(
        """package com.haleydu.cimoc.manager;

import java.util.HashMap;
import java.util.Map;

/** In-memory prefs shim (HHAAZZ/MH50 baseUrl keys). */
public class PreferenceManager {
    public static final String PREF_HHAAZZ_BASEURL = "PREF_HHAAZZ_BASEURL";
    public static final String PREF_HHAAZZ_SW = "PREF_HHAAZZ_SW";
    public static final String PREFERENCES_MH50_KEY_MSG = "PREFERENCES_MH50_KEY_MSG";
    public static final String PREFERENCES_MH50_IV_MSG = "PREFERENCES_MH50_IV_MSG";

    private final Map<String, String> map = new HashMap<>();

    public String getString(String key, String def) {
        String v = map.get(key);
        return v != null ? v : def;
    }

    public void putString(String key, String value) {
        map.put(key, value);
    }
}
""",
        encoding="utf-8",
    )

    # SourceManager: copy constants + getParser from original, strip Android Context/DB
    sm_src = (SRC / "manager" / "SourceManager.java").read_text(encoding="utf-8")
    # Keep file but we'll write a simplified version by extracting TYPE cases from source files
    (mgr / "SourceManager.java").write_text(build_source_manager(), encoding="utf-8")

    (DST / "core").mkdir(parents=True, exist_ok=True)
    (DST / "core" / "Manga.java").write_text(build_manga_core(), encoding="utf-8")

    # Hex shim (facebook fresco)
    hex_dir = DST.parent.parent.parent.parent / "com" / "facebook" / "common" / "util"
    # Actually package is under app/src/main/java/com/facebook/...
    fb = Path(
        r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\facebook\common\util"
    )
    fb.mkdir(parents=True, exist_ok=True)
    (fb / "Hex.java").write_text(
        """package com.facebook.common.util;

public final class Hex {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hex() {}

    public static String encodeHex(byte[] data, boolean upper) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        String s = new String(out);
        return upper ? s.toUpperCase() : s;
    }

    public static byte[] decodeHex(String s) {
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
""",
        encoding="utf-8",
    )

    jcc = Path(
        r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\taobe\tec\jcc"
    )
    jcc.mkdir(parents=True, exist_ok=True)
    (jcc / "JChineseConvertor.java").write_text(
        """package taobe.tec.jcc;

/** Identity stub for Cimoc CopyMH/HotManga (full OpenCC optional later). */
public final class JChineseConvertor {
    private static final JChineseConvertor INSTANCE = new JChineseConvertor();

    public static JChineseConvertor getInstance() {
        return INSTANCE;
    }

    public String s2t(String s) { return s; }
    public String t2s(String s) { return s; }
}
""",
        encoding="utf-8",
    )

    ui = Path(
        r"D:\cursor\browsesth\upstream\legado-with-MD3\app\src\main\java\com\haleydu\cimoc\ui\activity"
    )
    ui.mkdir(parents=True, exist_ok=True)
    (ui / "ResultActivity.java").write_text(
        """package com.haleydu.cimoc.ui.activity;

/** Unused stub for PuFei import. */
public class ResultActivity {}
""",
        encoding="utf-8",
    )


def build_source_manager() -> str:
    """Generate SourceManager with all web parsers from SourceManager switch."""
    text = (SRC / "manager" / "SourceManager.java").read_text(encoding="utf-8")
    # Extract SOURCE_ constants
    consts = "\n".join(
        line
        for line in text.splitlines()
        if "public static final int SOURCE_" in line or "public static final int TYPE" in line
    )
    # Simpler: include full getParser by copying file and stripping greenDAO/Context dependencies
    # Read original getParser method body via regex
    return f"""package com.haleydu.cimoc.manager;

import android.util.SparseArray;

import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.Parser;
import com.haleydu.cimoc.source.*;

/**
 * Vendored SourceManager (SPEC-016). No DB — constructs Source stubs.
 */
public class SourceManager {{

    public static final int SOURCE_UNKNOW = -999;
    public static final int SOURCE_LOCAL = -1;
    public static final int SOURCE_EXHENTAI = 60; // may differ; ExHentai uses own TYPE
    public static final int SOURCE_NHENTAI = 61;
    public static final int SOURCE_177PIC = 62;
    public static final int SOURCE_WNACG = 63;

{extract_source_constants(text)}

    private static SourceManager instance;
    private final SparseArray<Parser> mParserArray = new SparseArray<>();

    public static synchronized SourceManager getInstance() {{
        if (instance == null) {{
            instance = new SourceManager();
        }}
        return instance;
    }}

    private SourceManager() {{}}

    private Source stub(int type, String title) {{
        return new Source(null, title, type, true);
    }}

    public Parser getParser(int type) {{
        Parser parser = mParserArray.get(type);
        if (parser == null) {{
            Source source = stub(type, "source-" + type);
            switch (type) {{
{extract_switch_cases(text)}
                default:
                    parser = new Null();
                    break;
            }}
            mParserArray.put(type, parser);
        }}
        return parser;
    }}

    /** All registered web parser types (excludes Locality). */
    public static int[] allWebTypes() {{
        return new int[] {{
{extract_web_types(text)}
        }};
    }}
}}
"""


def extract_source_constants(text: str) -> str:
    lines = []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("public static final int SOURCE_"):
            lines.append("    " + s)
    return "\n".join(lines)


def extract_switch_cases(text: str) -> str:
    # From original SourceManager getParser switch
    m = re.search(
        r"switch \(type\) \{(.*?)\n\s*default:",
        text,
        re.S,
    )
    if not m:
        raise SystemExit("cannot find switch(type) in SourceManager")
    body = m.group(1)
    # Replace `parser = new X(source);` keep; skip Locality special if needed
    out = []
    for line in body.splitlines():
        if "Locality" in line and "case" in line:
            continue
        if "parser = new Locality" in line:
            continue
        if line.strip().startswith("//"):
            continue
        out.append(line)
    return "\n".join(out)


def extract_web_types(text: str) -> str:
    cases = re.findall(r"case (\w+)\.TYPE:", text)
    names = []
    for c in cases:
        if c == "Locality":
            continue
        names.append(f"            {c}.TYPE,")
    return "\n".join(names)


def build_manga_core() -> str:
    return r'''package com.haleydu.cimoc.core;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Synchronous subset of Cimoc Manga core for browsesth (SPEC-016).
 */
public final class Manga {

    public static class NetworkErrorException extends Exception {
        public NetworkErrorException() { super("network"); }
        public NetworkErrorException(String msg) { super(msg); }
    }

    public static class ParseErrorException extends Exception {
        public ParseErrorException() { super("parse"); }
    }

    private Manga() {}

    public static String getResponseBody(OkHttpClient client, Request request) throws NetworkErrorException {
        try {
            Response response = client.newCall(request).execute();
            if (response.body() == null) {
                throw new NetworkErrorException("empty body");
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new NetworkErrorException("HTTP " + response.code());
            }
            return body;
        } catch (NetworkErrorException e) {
            throw e;
        } catch (IOException e) {
            throw new NetworkErrorException(e.getMessage());
        }
    }

    public static List<Chapter> getComicInfoSync(Parser parser, Comic comic) throws Exception {
        comic.setUrl(parser.getUrl(comic.getCid()));
        Request request = parser.getInfoRequest(comic.getCid());
        String html = getResponseBody(App.getHttpClient(), request);
        parser.parseInfo(html, comic);
        Request chapterReq = parser.getChapterRequest(html, comic.getCid());
        if (chapterReq != null) {
            html = getResponseBody(App.getHttpClient(), chapterReq);
        }
        Long sourceComic = Long.parseLong(comic.getSource() + "000" + (comic.getId() == null ? "00" : comic.getId()));
        List<Chapter> list = parser.parseChapter(html, comic, sourceComic);
        if (list == null) {
            list = parser.parseChapter(html);
        }
        if (list == null || list.isEmpty()) {
            throw new ParseErrorException();
        }
        return list;
    }

    public static List<ImageUrl> getChapterImageSync(Parser parser, Chapter chapter, String cid, String path) throws Exception {
        Request request = parser.getImagesRequest(cid, path);
        String html = getResponseBody(App.getHttpClient(), request);
        List<ImageUrl> list = parser.parseImages(html, chapter);
        if (list == null || list.isEmpty()) {
            list = parser.parseImages(html);
        }
        if (list == null || list.isEmpty()) {
            throw new ParseErrorException();
        }
        for (ImageUrl imageUrl : list) {
            imageUrl.setChapter(path);
        }
        return list;
    }

    public static String loadLazyUrlSync(Parser parser, String url) throws Exception {
        Request request = parser.getLazyRequest(url);
        if (request == null) {
            return url;
        }
        String html = getResponseBody(App.getHttpClient(), request);
        String out = parser.parseLazy(html, url);
        return out != null ? out : url;
    }
}
'''


def fix_utils() -> None:
    # HttpUtils / LogUtil may pull Android UI — leave as copied; fix compile errors later
    pass


def main() -> None:
    copy_tree()
    for name in ("Chapter.java", "Comic.java", "ImageUrl.java", "Source.java", "MiniComic.java"):
        p = DST / "model" / name
        if p.exists():
            strip_greendao(p)
    write_shims()
    print("DONE vendor ->", DST)


if __name__ == "__main__":
    main()
