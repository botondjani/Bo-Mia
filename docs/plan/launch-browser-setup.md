# Prompt a böngészős Claude-nak: tend.hu indítási beállítások

Másold be egy Claude in Chrome (böngészős) beszélgetésbe. Ő a te bejelentkezett böngésződben dolgozik.

---

A tend nevű Android app (csomagnév: `app.tend`) indításához kell pár beállítást elvégezned a böngészőmben. Minden fiókba be vagyok jelentkezve. Dolgozz lépésenként, és minden lépés után ellenőrizd, hogy elmentődött.

**Szabályok**
- Csak az alább felsorolt beállításokhoz nyúlj. Semmit ne törölj, és ne válts fizetős csomagra, mielőtt megkérdezel.
- Az API-kulcsot soha ne írd ki a chatbe, és ne mentsd fájlba. Létrehozás után másold közvetlenül a Supabase mezőjébe.
- Ha egy lépés még nem végezhető el (pl. a domain DNS-e még nem él), ugord át, és a végén írd le, miért maradt ki.
- Ha valami eltér attól, amit leírtam (más menünév, már beállított érték), ne találgass: írd le, mit látsz, és kérdezz.

**1. Rackhost – DNS ellenőrzése** (rackhost.hu → Domainek → tend.hu → DNS)
- Nézd meg, hogy ezek a rekordok pontosan így szerepelnek-e:
  - `tend.hu` A `199.36.158.100`
  - `tend.hu` TXT `hosting-site=tend-5244d`
  - `www` CNAME `tend-5244d.web.app`
  - `resend._domainkey` TXT: a DKIM kulcs a Resend Domains oldaláról
  - `send` TXT `v=spf1 include:amazonses.com ~all`
  - `send` MX 10 `feedback-smtp.eu-west-1.amazonses.com`
  - `rsend` CNAME `send.forge.rmta.net`
  - `_dmarc` TXT `v=DMARC1; p=none;`
- Ha valamelyik hiányzik vagy eltér, javítsd: a pontos értéket a Resend és a Firebase oldaláról vedd.
- Nézd meg, lezárult-e már a domain regisztrációja.

**2. Resend** (resend.com)
- Domains → tend.hu: ha még nem „Verified”, nyomd meg a Verify gombot. Ha még így sem zöld, jegyezd fel, és menj tovább.
- API Keys → Create API key:
  - Név: `tend-supabase-smtp`, jogosultság: Sending access, domain: tend.hu.
  - A kulcsot csak a vágólapra másold, és a 3. lépésben illeszd be.

**3. Supabase** (supabase.com/dashboard, „Bo & Mia” projekt, azonosító: `tnrwkgcjqbnewsnfbkvi`)
- Authentication → Emails → SMTP Settings → Enable custom SMTP:
  - Sender email: `noreply@tend.hu`, sender name: `tend`
  - Host: `smtp.resend.com`, port: `465`, username: `resend`, password: a 2. lépés API-kulcsa
  - Mentsd el.
- Authentication → Emails → Templates → Magic Link: a levél törzsében legyen benne a `{{ .Token }}` (az app 6 jegyű kódot vár).
  - Ha nincs benne, a törzs legyen ez: `<h2>A tend belépési kódod</h2><p>Írd be ezt a kódot az appban: <strong>{{ .Token }}</strong></p><p>Ha nem te kérted, nyugodtan hagyd figyelmen kívül.</p>`
  - Tárgy: `A tend belépési kódod`
- Authentication → Rate Limits: az e-mailek óránkénti limitje legyen 100.
- Authentication → URL Configuration: Site URL legyen `https://tend.hu`.
- Ha minden kész, küldj egy próbalevelet, ha a felület enged ilyet. Ha nem, írd meg, és az appból kipróbálom.

**4. Google Cloud Console** (console.cloud.google.com; az a projekt, amelyikben ez az OAuth Web client van: `1051107306969-u9i4buqc5p06uif2hujbqd7ivhehl40r.apps.googleusercontent.com`)
- Google Auth Platform / OAuth consent screen:
  - Authorized domains: add hozzá a `tend.hu`-t.
  - App home page: `https://tend.hu/`
  - Privacy policy: `https://tend.hu/privacy/`
  - Terms of service: `https://tend.hu/terms/`
  - Ha a publikálási állapot „Testing”, írd le, és kérdezz, mielőtt „In production”-re állítod.
- Clients / Credentials: nézd meg, van-e Android típusú kliens `app.tend` csomagnévvel.
  - Ha nincs, hozz létre egyet ezzel az SHA-1-gyel (feltöltési kulcs): `A0:F3:52:F8:87:8E:1F:29:93:42:EF:65:DC:5C:99:86:13:FE:67:8F`
  - Ha az 5. lépésben megvan a Play aláírókulcs SHA-1-e, azzal hozz létre egy második Android klienst is.

**5. Google Play Console** (play.google.com/console)
- Ha a tend app már létre van hozva:
  - Tesztelés és kiadás → Alkalmazásintegritás → Alkalmazás-aláírás: másold ki az **alkalmazás-aláíró kulcs** SHA-1 és SHA-256 ujjlenyomatát. Ezek nem titkosak, a végén írd ki őket a chatbe.
  - Az SHA-1-gyel végezd el a 4. lépés második Android kliensét.
  - Alkalmazástartalom → Adatvédelmi irányelvek: `https://tend.hu/privacy/`
- Ha az app még nincs létrehozva, ezt a lépést hagyd ki.

**6. Firebase** (console.firebase.google.com, `tend` projekt, `tend-5244d`)
- Hosting → Custom domains: írd le a `tend.hu` és a `www.tend.hu` állapotát. Ha olyan rekordot kér, ami nincs a Rackhostnál, add hozzá.
- Semmi mást ne állíts, a deployt én csinálom a gépemről.

**A végén** írj egy rövid táblázatot: lépés, kész vagy nem, és ha nem, miért. Alá írd ki a Play aláírókulcs SHA-1 és SHA-256 ujjlenyomatát, ha megvan.
