import re, sys, json
from lex import *
UI = re.compile(r'\b(Text|Pill|ChipButton|PrimaryButton|SecondaryButton|Notice|SectionHeader|Toast|Title|LinkText|SwitchRow|Feature|HoldToConfirm|IconButtonSoft|setContentTitle|setContentText|bigText|hint|contentDescription|OpenChip|Explain|StepPage|cta)\b')
KEYCTX = re.compile(r'(getString|putString|getBoolean|putBoolean|getInt|putInt|getLong|putLong|getFloat|putFloat|getStringSet|putStringSet|contains|remove|Stats\.log|optString|optInt|optLong|optBoolean|optDouble|optJSONObject|optJSONArray|getJSONObject|\.put|put|rpc|Regex|Log\.[wde]|getSharedPreferences|setRequestProperty|logMiss|logAnswer|File|NotificationChannel|Badge|Glyph|play|label =|Color|startsWith|endsWith|substringAfter|substringBefore|split|joinToString|trim|format|require|error|check|Uri\.parse|Intent|putExtra|getStringExtra|getBooleanExtra|addAcorns|append|lines|setNumberState|setBooleanState|fireState|Thing|TaskDef|Triple|parse)\s*\($')
seen = {}
for f in files():
    s = open(f).read()
    rel = f.split('/app/src/')[1].replace('java/app/tend/', '')
    for a, b in literals(s):
        lit = s[a:b]
        st = static_text(lit)
        if not re.search(r'[A-Za-zÀ-ž]{2}', st): continue
        ls = s.rfind('\n', 0, a) + 1; le = s.find('\n', a); line = s[ls:le]
        if line.lstrip().startswith(('import ', 'package ', '@')): continue
        before = s[max(0, a - 40):a]
        word = st.strip('· ')
        looks = (' ' in st.strip()) or re.match(r'^[A-Z][a-z]', word) or re.match(r'^[A-Z]{3,}$', word) or (UI.search(line) and re.search(r'[a-z]{2}', st))
        if not looks: continue
        # ids and keys: lowercase_with_underscores, or a key-ish call right before it
        if re.fullmatch(r'[a-z0-9_:.\-/%]+', st.strip()) and not UI.search(line): continue
        if re.search(r'"\s*->', s[b - 1:b + 6]) and re.fullmatch(r'[a-z0-9_]+', st): continue
        key = lit
        e = seen.setdefault(key, {'n': 0, 'where': [], 'ctx': line.strip()[:150], 'keyctx': bool(KEYCTX.search(before))})
        e['n'] += 1
        if len(e['where']) < 3: e['where'].append(f'{rel}:{s.count(chr(10), 0, a) + 1}')
items = [{'lit': k, **v} for k, v in seen.items()]
json.dump(items, open('strings.json', 'w'), ensure_ascii=False, indent=0)
print(len(items), sum(i['n'] for i in items), sum(len(i['lit']) for i in items))
