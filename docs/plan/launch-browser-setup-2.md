# Prompt a böngészős Claude-nak: tend.hu automatikus deploy

Másold be egy Claude in Chrome (böngészős) beszélgetésbe.

---

A tend.hu weboldalt (Firebase Hosting, projekt: `tend-5244d`) a GitHub automatikusan publikálja a `botondjani/Bo-Mia` repó `main` ágából. Ehhez egy titkos kulcsot kell beállítanod. Be vagyok jelentkezve a Google-be és a GitHubra.

**Szabályok**
- A kulcs tartalmát ne írd ki a chatbe, és ne mentsd sehova máshova, csak a GitHub titok mezőjébe.
- A letöltött JSON fájlt a végén töröld a Letöltések mappából, ha a böngészőből meg tudod tenni. Ha nem, szólj, és én törlöm.
- Mást ne változtass. Ha valami nem úgy néz ki, ahogy leírtam, kérdezz.

**1. Szolgáltatásfiók-kulcs** (console.firebase.google.com → `tend` projekt → fogaskerék → Project settings → Service accounts)
- „Generate new private key” → letölt egy JSON fájlt.
- Ez a fiók (firebase-adminsdk-…) Firebase-adminisztrátori jogot kap, a hostinghoz ez elég.

**2. GitHub titok** (github.com/botondjani/Bo-Mia → Settings → Secrets and variables → Actions → New repository secret)
- Név: `FIREBASE_SERVICE_ACCOUNT_TEND_5244D`
- Érték: a letöltött JSON fájl teljes tartalma.

**3. Indítás** (github.com/botondjani/Bo-Mia → Actions → „Deploy tend.hu” → Run workflow → Branch: main)
- Ha a „Deploy tend.hu” nem látszik, a `main` ágban még nincs benne a `.github/workflows/hosting.yml`. Ilyenkor szólj, ne indíts semmit.
- Várd meg, hogy zöld legyen, aztán nyisd meg a https://tend.hu/ és a https://tend.hu/privacy/ oldalt, és írd meg, betöltenek-e.
