# SPEC-035: Material Symbols replace + keep Material branch of Miuix if/else.
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(r"D:\cursor\browsesth\upstream\legado-with-MD3")
SRC = ROOT / "app" / "src"
ICON_DIR = SRC / "main" / "java" / "io" / "legado" / "app" / "ui" / "widget" / "components" / "icon"

SEMANTIC = {
    "FilterNone": "tabs",
    "AutoStories": "menu_book",
    "PhoneAndroid": "smartphone",
    "InsertDriveFile": "draft",
    "ExitToApp": "logout",
    "MenuOpen": "menu_open",
    "PauseCircleOutline": "pause_circle",
    "AddCircleOutline": "add_circle",
    "DriveFileRenameOutline": "drive_file_rename_outline",
    "FormatListBulleted": "format_list_bulleted",
    "NoteAdd": "note_add",
}

MIRROR = {
    "ArrowBack",
    "ArrowForward",
    "ArrowLeft",
    "ArrowRight",
    "ChevronRight",
    "KeyboardArrowLeft",
    "KeyboardArrowRight",
    "ExitToApp",
    "NoteAdd",
    "LibraryBooks",
    "FormatListBulleted",
    "Redo",
    "Undo",
    "Login",
    "OpenInNew",
}

ICON_RE = re.compile(
    r"Icons\.(?:AutoMirrored\.)?(?:Default|Filled|Outlined|Rounded|Sharp|TwoTone)\.(\w+)"
)


def pascal_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def collect_names() -> set[str]:
    names: set[str] = set()
    for p in SRC.rglob("*.kt"):
        names.update(ICON_RE.findall(p.read_text(encoding="utf-8", errors="ignore")))
    return names


def write_symbols(names: set[str]) -> None:
    lines = [
        "package io.legado.app.ui.widget.components.icon",
        "",
        "/** Material Symbols ligatures. Axes are applied in [SymbolIcon]. */",
        "object Symbols {",
    ]
    for n in sorted(names):
        lig = SEMANTIC.get(n, pascal_to_snake(n))
        mirror = "true" if n in MIRROR else "false"
        lines.append(f'    val {n} = MaterialSymbol("{lig}", autoMirror = {mirror})')
    lines.append("}")
    lines.append("")
    (ICON_DIR / "Symbols.kt").write_text("\n".join(lines), encoding="utf-8")


def match_paren(s: str, i: int) -> int:
    assert s[i] == "("
    depth = 0
    for j in range(i, len(s)):
        if s[j] == "(":
            depth += 1
        elif s[j] == ")":
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced paren")


def match_brace(s: str, i: int) -> int:
    assert s[i] == "{"
    depth = 0
    for j in range(i, len(s)):
        if s[j] == "{":
            depth += 1
        elif s[j] == "}":
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced brace")


def skip_ws(s: str, i: int) -> int:
    while i < len(s) and s[i] in " \t\r\n":
        i += 1
    return i


MIUIX_CALL = re.compile(
    r"ThemeResolver\.isMiuixEngine\s*\("
)


def strip_miuix_if(s: str) -> str:
    """Keep Material branch of if (isMiuixEngine) / if (isMiuix). Repeat until stable."""
    changed = True
    guard = 0
    while changed and guard < 80:
        guard += 1
        changed = False
        # if (ThemeResolver.isMiuixEngine(...))
        m = MIUIX_CALL.search(s)
        while m:
            # walk back to 'if'
            k = m.start()
            prefix = s[:k].rstrip()
            # handle `if (ThemeResolver...` or `xxx && ThemeResolver` — only simple if (
            if_idx = prefix.rfind("if")
            if if_idx == -1:
                break
            between = prefix[if_idx + 2 :]
            if not between.strip().startswith("(") and "if (" not in s[if_idx:m.start()]:
                # find `if (` immediately
                pass
            # Find the if keyword that owns this condition
            window = s[max(0, m.start() - 80) : m.start()]
            if_m = list(re.finditer(r"\bif\s*\(", window))
            if not if_m:
                # maybe `foo && ThemeResolver` inside if already handled
                m = MIUIX_CALL.search(s, m.end())
                continue
            if_abs = max(0, m.start() - 80) + if_m[-1].start()
            cond_open = s.find("(", if_abs)
            try:
                cond_close = match_paren(s, cond_open)
            except ValueError:
                break
            cond = s[cond_open : cond_close + 1]
            negated = bool(re.search(r"!\s*ThemeResolver\.isMiuixEngine", cond)) or cond.strip().startswith("(!")
            # only rewrite if condition is solely isMiuixEngine (optionally negated)
            simple = re.fullmatch(
                r"\(\s*!?\s*ThemeResolver\.isMiuixEngine\s*\(.*\)\s*\)",
                cond,
                flags=re.S,
            )
            if not simple:
                m = MIUIX_CALL.search(s, m.end())
                continue
            j = skip_ws(s, cond_close + 1)
            if j < len(s) and s[j] == "{":
                a_end = match_brace(s, j)
                a_body = s[j + 1 : a_end]
                k2 = skip_ws(s, a_end + 1)
                if k2 + 4 <= len(s) and s[k2 : k2 + 4] == "else":
                    k3 = skip_ws(s, k2 + 4)
                    if s[k3] == "{":
                        b_end = match_brace(s, k3)
                        b_body = s[k3 + 1 : b_end]
                        keep = a_body if negated else b_body
                        s = s[:if_abs] + keep.strip() + s[b_end + 1 :]
                    else:
                        # else expr
                        # take until newline that's not continuation — crude: until next newline at same indent
                        rest = s[k3:]
                        line = rest.split("\n", 1)
                        expr = line[0]
                        tail = line[1] if len(line) > 1 else ""
                        keep = s[j + 1 : a_end] if negated else expr
                        s = s[:if_abs] + keep.strip() + ("\n" + tail if tail else "")
                else:
                    # no else: drop miuix block, keep material if negated
                    keep = a_body.strip() if negated else ""
                    s = s[:if_abs] + keep + s[a_end + 1 :]
                changed = True
                break
            else:
                # if (cond) expr else expr
                rest = s[j:]
                else_m = re.match(r"(.+?)\s+else\s+", rest, flags=re.S)
                if not else_m:
                    m = MIUIX_CALL.search(s, m.end())
                    continue
                then_expr = else_m.group(1).strip()
                after_else = skip_ws(s, j + else_m.end())
                # expression until newline
                nl = s.find("\n", after_else)
                else_expr = s[after_else:nl] if nl != -1 else s[after_else:]
                keep = then_expr if negated else else_expr.strip()
                end = nl if nl != -1 else len(s)
                s = s[:if_abs] + keep + s[end:]
                changed = True
                break
        else:
            break
    return s


ISMIUIX_VAL = re.compile(
    r"\n[ \t]*val isMiuix\s*=\s*ThemeResolver\.isMiuixEngine\([^)]*\)[ \t]*\n"
)


def strip_is_miuix_val_if(s: str) -> str:
    """if (isMiuix) { A } else { B } -> B after val isMiuix = ..."""
    s = ISMIUIX_VAL.sub("\n", s)
    changed = True
    guard = 0
    while changed and guard < 40:
        guard += 1
        changed = False
        for m in list(re.finditer(r"\bif\s*\(\s*!?\s*isMiuix\s*\)", s)):
            negated = "!" in m.group(0)
            if_abs = m.start()
            cond_open = s.find("(", if_abs)
            cond_close = match_paren(s, cond_open)
            j = skip_ws(s, cond_close + 1)
            if j < len(s) and s[j] == "{":
                a_end = match_brace(s, j)
                a_body = s[j + 1 : a_end]
                k2 = skip_ws(s, a_end + 1)
                if k2 + 4 <= len(s) and s[k2 : k2 + 4] == "else":
                    k3 = skip_ws(s, k2 + 4)
                    if k3 < len(s) and s[k3] == "if":
                        # else if — skip this pattern
                        continue
                    if k3 < len(s) and s[k3] == "{":
                        b_end = match_brace(s, k3)
                        b_body = s[k3 + 1 : b_end]
                        keep = a_body if negated else b_body
                        s = s[:if_abs] + keep.strip() + s[b_end + 1 :]
                        changed = True
                        break
                else:
                    keep = a_body.strip() if negated else ""
                    s = s[:if_abs] + keep + s[a_end + 1 :]
                    changed = True
                    break
            else:
                rest = s[j:]
                else_m = re.match(r"(.+?)\s+else\s+", rest, flags=re.S)
                if not else_m:
                    continue
                then_expr = else_m.group(1).strip()
                after_else = j + else_m.end()
                nl = s.find("\n", after_else)
                else_expr = s[after_else:nl] if nl != -1 else s[after_else:]
                keep = then_expr if negated else else_expr.strip()
                end = nl if nl != -1 else len(s)
                s = s[:if_abs] + keep + s[end:]
                changed = True
                break
    return s


def process_icons(text: str) -> str:
    if "androidx.compose.material.icons" not in text and "Icons." not in text:
        return text
    text = ICON_RE.sub(r"Symbols.\1", text)
    text = re.sub(r"^import androidx\.compose\.material\.icons\.[^\n]+\n", "", text, flags=re.M)
    text = re.sub(r"^import androidx\.compose\.material\.icons\n", "", text, flags=re.M)
    if "Symbols." in text:
        if "import io.legado.app.ui.widget.components.icon.Symbols" not in text:
            text = text.replace(
                "package ",
                "package ",
                1,
            )
            lines = text.split("\n", 1)
            rest = lines[1] if len(lines) > 1 else ""
            extra = (
                "import io.legado.app.ui.widget.components.icon.SymbolIcon\n"
                "import io.legado.app.ui.widget.components.icon.Symbols\n"
            )
            # insert after package line
            text = lines[0] + "\n\n" + extra + rest.lstrip("\n")
            # de-dup later
    text = text.replace("imageVector = Symbols.", "symbol = Symbols.")
    text = re.sub(r"\bIcon\(\s*symbol\s*=", "SymbolIcon(symbol =", text)
    text = re.sub(r"\bIcon\(\s*Symbols\.", "SymbolIcon(Symbols.", text)
    text = text.replace("imageVector: ImageVector", "symbol: MaterialSymbol")
    text = text.replace("icon: ImageVector", "icon: MaterialSymbol")
    text = text.replace("iconVector: ImageVector", "iconVector: MaterialSymbol")
    if "MaterialSymbol" in text and "import io.legado.app.ui.widget.components.icon.MaterialSymbol" not in text:
        text = text.replace(
            "import io.legado.app.ui.widget.components.icon.Symbols\n",
            "import io.legado.app.ui.widget.components.icon.MaterialSymbol\n"
            "import io.legado.app.ui.widget.components.icon.Symbols\n",
        )
    # drop unused ImageVector import if no longer referenced
    if "ImageVector" not in text.replace("import androidx.compose.ui.graphics.vector.ImageVector", ""):
        text = text.replace("import androidx.compose.ui.graphics.vector.ImageVector\n", "")
    return text


def strip_miuix_imports(text: str) -> str:
    text = re.sub(r"^import top\.yukonga\.miuix\.[^\n]+\n", "", text, flags=re.M)
    return text


def main() -> None:
    names = collect_names()
    write_symbols(names)
    print("symbols", len(names))
    for p in list(SRC.rglob("*.kt")):
        orig = p.read_text(encoding="utf-8")
        t = orig
        t = process_icons(t)
        t = strip_miuix_if(t)
        t = strip_is_miuix_val_if(t)
        t = strip_miuix_imports(t)
        # collapse duplicate imports
        seen = set()
        out_lines = []
        for line in t.splitlines(True):
            if line.startswith("import "):
                if line in seen:
                    continue
                seen.add(line)
            out_lines.append(line)
        t = "".join(out_lines)
        if t != orig:
            p.write_text(t, encoding="utf-8")
            print("updated", p.relative_to(ROOT))


if __name__ == "__main__":
    main()
