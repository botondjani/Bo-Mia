# Prompt a böngészős Claude-nak: automatikus Play-kiadás

Másold be egy Claude in Chrome beszélgetésbe, és csatold hozzá a két fájlt: `TEND_UPLOAD_KEYSTORE_B64.txt` és `tend-upload-jelszavak.txt`.

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
- A csatolt fájlokból még három titok:
  - `TEND_UPLOAD_KEYSTORE_B64`: a `TEND_UPLOAD_KEYSTORE_B64.txt` teljes tartalma (egyetlen hosszú sor).
  - `TEND_KEYSTORE_PASSWORD`: a `tend-upload-jelszavak.txt` fájlban a „TEND_KEYSTORE_PASSWORD” alatti sor.
  - `TEND_KEY_PASSWORD`: ugyanott a „TEND_KEY_PASSWORD” alatti sor.
  - Ezek értékét se írd ki a chatbe.

**4. Próbakiadás** (github.com/botondjani/Bo-Mia → Actions → „Release to Google Play” → Run workflow)
- Branch: `main`, track: `internal`, notes: `Javítások és apró fejlesztések.`
- Várd meg a végét. Ha zöld: a Play Console → Belső tesztelés alatt meg kell jelennie egy új kiadásnak (versionCode 100 fölött). Ha piros: másold ki a hibaüzenet utolsó pár sorát (titkot ne).
- A zárt tesztet (Alpha) most ne indítsd el.

**A végén** írd le, mi lett kész, megvan-e mind a négy titok, és mi lett a próbakiadás eredménye.
