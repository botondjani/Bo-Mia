# Válasz a böngészős Claude-nak: belső teszt közzététele

---

Köszönöm, minden rendben. A döntéseim:

**1. Android OAuth-kliens a „korábbi” kulcshoz: igen.**
Google Cloud → Clients → Create client → Android:
- Név: `tend Android hu.tend (Play signing, earlier key)`
- Csomagnév: `hu.tend`
- SHA-1: `E4:00:72:FE:FC:2B:79:82:EA:34:E9:0B:F5:E2:CE:83:C7:3C:96:26`

A meglévő klienseket ne töröld, az `app.tend`-et se: a kézzel telepített tesztverzió még azt használja.

**2. A belső teszt kiadást tedd közzé.**
- Tesztelők: hozz létre egy listát „tend belső teszt” néven, egyetlen címmel: `botondjani@gmail.com`. Másokat később én adok hozzá.
- Kiadás neve: `1.0.0 (1)`
- Kiadási megjegyzés, magyarul (hu-HU):
  `Első belső tesztverzió: appkorlát, szünet a választott appok előtt, Telefon le, tábor és barátok.`
- Ha angol (en-US) megjegyzést is kér:
  `First internal test build: app limits, a pause before chosen apps, Phone down, the camp and friends.`
- Ha a Play a közzététel előtt további kötelező adatot kér (pl. adatkezelési űrlap, tartalmi besorolás, célközönség), ne találgass: írd le pontosan, mit kér, és állj meg.

**3. Az `assetlinks.json` már kész:** mindhárom Play SHA-256 benne van a feltöltési kulcsé mellett, és kint van a https://tend.hu/.well-known/assetlinks.json címen.

**A végén** küldd el a belső teszt meghívólinkjét (Tesztelők fül → „Copy link”), hogy telefonról telepíthessem.
