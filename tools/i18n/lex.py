"""Finds top-level Kotlin string literals (with ${} nesting) in the app's sources."""
import glob, os, re
ROOT = '/home/user/Bo-Mia/app/src'
def files():
    out = []
    for d in ('main', 'play', 'sideload'):
        out += glob.glob(f'{ROOT}/{d}/java/**/*.kt', recursive=True)
    return sorted(out)

def skip_string(s, i):
    """s[i] == '"' (not raw). Returns index after the closing quote."""
    i += 1
    while i < len(s):
        c = s[i]
        if c == '\\': i += 2; continue
        if c == '"': return i + 1
        if c == '$' and i + 1 < len(s) and s[i + 1] == '{':
            i = skip_code(s, i + 2, True); continue
        i += 1
    return i

def skip_code(s, i, until_brace):
    """Skips code from i; with until_brace, returns after the matching '}'."""
    depth = 0
    while i < len(s):
        c = s[i]
        if s.startswith('//', i):
            i = s.find('\n', i); i = len(s) if i < 0 else i; continue
        if s.startswith('/*', i):
            i = s.find('*/', i) + 2; continue
        if s.startswith('"""', i):
            i = s.find('"""', i + 3) + 3; continue
        if c == '"': i = skip_string(s, i); continue
        if c == "'":
            j = i + 1
            if s[j] == '\\': j += 1
            j = s.find("'", j + 1)
            i = j + 1; continue
        if c == '{': depth += 1
        if c == '}':
            if until_brace and depth == 0: return i + 1
            depth -= 1
        i += 1
    return i

def literals(s):
    """Yields (start, end) of top-level normal string literals."""
    i = 0
    while i < len(s):
        c = s[i]
        if s.startswith('//', i):
            i = s.find('\n', i); i = len(s) if i < 0 else i; continue
        if s.startswith('/*', i):
            i = s.find('*/', i) + 2; continue
        if s.startswith('"""', i):
            i = s.find('"""', i + 3) + 3; continue
        if c == '"':
            j = skip_string(s, i); yield (i, j); i = j; continue
        if c == "'":
            j = i + 1
            if s[j] == '\\': j += 1
            j = s.find("'", j + 1)
            i = j + 1; continue
        i += 1

def static_text(lit):
    """The literal without its ${...} parts and $names."""
    out = []; i = 1
    while i < len(lit) - 1:
        if lit[i] == '\\': out.append(' '); i += 2; continue
        if lit[i] == '$' and lit[i + 1] == '{':
            i = skip_code(lit, i + 2, True); out.append('·'); continue
        if lit[i] == '$' and (lit[i + 1].isalpha() or lit[i + 1] == '_'):
            i += 1
            while lit[i].isalnum() or lit[i] == '_': i += 1
            out.append('·'); continue
        out.append(lit[i]); i += 1
    return ''.join(out)
