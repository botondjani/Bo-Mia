"""Wraps translated literals: "en" -> tr("en", "hu"). Keyed by the English literal's source text."""
import json, glob, re, sys
from lex import *
items = json.load(open('strings.json'))
hu = {}
for f in sorted(glob.glob('hu*.tsv')):
    for line in open(f):
        line = line.rstrip('\n')
        if not line.strip(): continue
        i, t = line.split('\t', 1)
        hu[items[int(i)]['lit']] = t
print(len(hu), 'translations')
changed = 0
for f in files():
    s = open(f).read()
    out = []; last = 0; n = 0; skip_next = False
    for a, b in literals(s):
        lit = s[a:b]
        before = s[max(0, a - 3):a]
        if before == 'tr(':  # already written in both languages
            skip_next = True; continue
        if skip_next: skip_next = False; continue
        if lit not in hu: continue
        ls = s.rfind('\n', 0, a) + 1; line = s[ls:s.find('\n', a)]
        st = line.lstrip()
        if st.startswith(('import ', 'package ', '@', 'const val', 'private const val', 'internal const val')): continue
        # when-branch keys and comparisons stay as they are
        if re.match(r'\s*(->|,\s*"[^"]*"\s*->)', s[b:b + 40]) and not re.search(r'[=(]\s*$', s[max(0, a - 3):a]) and re.fullmatch(r'\s*', s[ls:a]): continue
        if re.search(r'(==|!=|\bin)\s*$', s[max(0, a - 6):a]): continue
        out.append(s[last:a]); out.append('tr(' + lit + ', ' + hu[lit] + ')'); last = b; n += 1
    if n:
        out.append(s[last:]); s2 = ''.join(out)
        if not f.endswith('/core/Lang.kt') and '\npackage app.tend.core\n' not in '\n' + s2 and 'import app.tend.core.tr\n' not in s2:
            s2 = re.sub(r'(\npackage [^\n]+\n)', r'\1\nimport app.tend.core.tr\n', '\n' + s2, count=1)[1:]
        open(f, 'w').write(s2); changed += n
print(changed, 'literals wrapped')
