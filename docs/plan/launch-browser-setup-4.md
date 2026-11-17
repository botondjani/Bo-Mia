# Prompt a böngészős Claude-nak: automatikus Play-kiadás

Másold be egy Claude in Chrome beszélgetésbe. A kulcsokat (keystore és jelszavak) Botond adja meg a GitHubon, nem te.

---

A tend Android app (`hu.tend`) új verzióit a GitHub fogja feltölteni a Google Playre. Ehhez egy szolgáltatásfiók kell, amivel a GitHub feltölthet. Minden fiókba be vagyok jelentkezve.

**Szabályok**
- A JSON kulcs tartalmát ne írd ki a chatbe, csak a GitHub titok mezőjébe illeszd be. A letöltött fájlt a végén töröld, ha a böngészőből meg tudod tenni.
- Mást ne változtass. Ha valami eltér a leírástól, kérdezz.

**1. Szolgáltatásfiók** (console.cloud.google.com, ugyanaz a projekt, amelyikben a tend OAuth-kliensei vannak)
- IAM & Admin → Service Accounts → Create: név `tend-play-release`. Szerepkör nem kell.
- A fiók → Keys → Add key → JSON → letöltődik.
- APIs & Services → Library: a „Google Play Android Developer API” legyen engedélyezve.

**2. Jogosultság a Play Console-ban** (play.google.com/console → Felhasználók és engedélyek → Új felhasználók meghívása)
- E-mail: a szolgáltatásfiók címe (`tend-play-release@…iam.gserviceaccount.com`).
- Alkalmazásengedély: csak a `hu.tend` app, ezekkel: „Kiadások megtekintése”, „Kiadás tesztcsatornákra”, „Kiadás éles környezetbe…”. Pénzügyi és felhasználói jogosultságot ne adj.

**3. GitHub titok** (github.com/botondjani/Bo-Mia → Settings → Secrets and variables → Actions → New repository secret)
- `PLAY_SERVICE_ACCOUNT_JSON`: a letöltött JSON fájl teljes tartalma.
- Nézd meg, megvan-e már ez a három titok: `TEND_UPLOAD_KEYSTORE_B64`, `TEND_KEYSTORE_PASSWORD`, `TEND_KEY_PASSWORD`. Ha nincs, ne hozd létre, csak jelezd: ezeket Botond adja meg.

**A végén** írd le, mi lett kész, és megvan-e mind a négy titok.
