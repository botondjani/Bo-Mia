// Renders Bo stills, store graphics and short Bo clips.
// Usage: node store.js <baseUrl> <outDir>
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const [base, out] = process.argv.slice(2);
fs.mkdirSync(out, { recursive: true });
const FONTS = {
  fredoka: fs.readFileSync(path.join(__dirname, '..', 'rivetest', 'fredoka.ttf')).toString('base64'),
  nunito: fs.readFileSync(path.join(__dirname, '..', 'rivetest', 'nunito.ttf')).toString('base64'),
};
const ONLY_GRAPHICS = process.argv[4] === 'graphics';
const SAGE = '#F1F4ED', MOSS = '#316849', INK = '#1D2A23', MUTED = '#6B756F';

async function still(browser, name, inputs, size = 1000) {
  const p = await browser.newPage({ viewport: { width: size, height: size } });
  await p.goto(`${base}/bo.html?size=${size}&in=${encodeURIComponent(JSON.stringify(inputs))}`);
  await p.waitForFunction(() => window.loaded, null, { timeout: 15000 });
  await p.waitForTimeout(900);
  const file = path.join(out, name);
  await p.screenshot({ path: file, omitBackground: true });
  await p.close();
  return file;
}

async function html(browser, name, w, h, body) {
  const p = await browser.newPage({ viewport: { width: w, height: h } });
  const page = `<!doctype html><html><head><style>
    @font-face{font-family:Fredoka;src:url(data:font/ttf;base64,${FONTS.fredoka})} @font-face{font-family:Nunito;src:url(data:font/ttf;base64,${FONTS.nunito})}
    html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden;font-family:Nunito}
  </style></head><body>${body}</body></html>`;
  await p.setContent(page, { waitUntil: 'networkidle' });
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(300);
  await p.screenshot({ path: path.join(out, name) });
  await p.close();
}

async function clip(browser, name, inputs, bg, taps = []) {
  const size = 1080;
  const dir = path.join(out, 'tmp-video');
  fs.mkdirSync(dir, { recursive: true });
  const ctx = await browser.newContext({ viewport: { width: size, height: size }, recordVideo: { dir, size: { width: size, height: size } } });
  const p = await ctx.newPage();
  const t0 = Date.now();
  await p.goto(`${base}/bo.html?size=${size}&bg=${encodeURIComponent(bg)}&in=${encodeURIComponent(JSON.stringify(inputs))}`);
  await p.waitForFunction(() => window.loaded, null, { timeout: 15000 });
  const start = (Date.now() - t0) / 1000 + 0.4;
  for (const at of taps) { await p.waitForTimeout(at); await p.evaluate(() => window.tap()); }
  await p.waitForTimeout(5200 - taps.reduce((a, b) => a + b, 0));
  const video = await p.video().path();
  await ctx.close();
  execFileSync('ffmpeg', ['-y', '-loglevel', 'error', '-ss', String(start), '-i', video, '-t', '5', '-r', '30',
    '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '18', '-movflags', '+faststart', path.join(out, name)]);
  fs.unlinkSync(video);
}

(async () => {
  const b = await chromium.launch();
  // Stills of Bo on a transparent background
  const happy = await still(b, 'bo-happy.png', { mood: 0 });
  const sci = await still(b, 'bo-scientist.png', { mood: 0, scientist: true });
  const url = f => `${base}/${path.basename(f)}`;
  for (const f of [happy, sci]) fs.copyFileSync(f, path.join(path.dirname(out), 'rivetest', path.basename(f)));

  // App icon 512: Bo on the sage-to-mint plate (Play applies its own rounded mask)
  await html(b, 'icon-512.png', 512, 512, `
    <div style="width:512px;height:512px;background:radial-gradient(circle at 50% 28%,#FFFFFF 0%,#D7EDDF 85%);display:flex;align-items:center;justify-content:center">
      <img src="${url(happy)}" style="width:430px;height:430px;margin-top:20px"></div>`);

  // Feature graphic 1024x500
  await html(b, 'feature-graphic.png', 1024, 500, `
    <div style="width:1024px;height:500px;background:linear-gradient(120deg,#FFFFFF 0%,${SAGE} 45%,#D7EDDF 100%);position:relative">
      <div style="position:absolute;left:70px;top:110px;width:540px">
        <div style="font-family:Fredoka;font-weight:600;font-size:96px;color:${MOSS};line-height:1">tend</div>
        <div style="font-family:Fredoka;font-weight:600;font-size:40px;color:${INK};margin-top:18px;line-height:1.15">A little pause before you scroll.</div>
        <div style="font-size:24px;color:${MUTED};margin-top:14px">Earn your minutes by living. Watch your garden grow.</div>
      </div>
      <img src="${url(sci)}" style="position:absolute;right:30px;top:60px;width:400px;height:400px">
    </div>`);

  // Phone screenshots 1080x1920: a headline and the screen in a soft phone frame
  const shots = [
    ['s-pause.png', 'A breath before', 'you scroll'],
    ['s-home.png', 'Earn minutes', 'by living'],
    ['s-timer.png', 'Do it together,', 'phone away'],
    ['s-garden.png', 'Unused minutes', 'grow a garden'],
    ['s-recap.png', 'A calm recap', 'every night'],
  ];
  for (const [i, [src, l1, l2]] of shots.entries()) {
    await html(b, `screenshot-${i + 1}.png`, 1080, 1920, `
      <div style="width:1080px;height:1920px;background:linear-gradient(180deg,#FFFFFF 0%,${SAGE} 35%,#D7EDDF 100%);position:relative;overflow:hidden">
        <div style="position:absolute;top:90px;width:100%;text-align:center;font-family:Fredoka;font-weight:600;font-size:84px;line-height:1.08;color:${INK}">
          ${l1}<br><span style="color:${MOSS}">${l2}</span></div>
        <div style="position:absolute;left:140px;top:420px;width:800px;height:1734px;border-radius:72px;background:#1D2A23;padding:18px;box-sizing:border-box;box-shadow:0 30px 80px rgba(29,42,35,.18)">
          <div style="width:100%;height:100%;border-radius:56px;overflow:hidden;background:${SAGE}">
            <img src="${base}/${src}" style="width:100%;display:block"></div></div>
      </div>`);
  }

  // Bo clips for short videos: brand background and green screen (key out #00FF00 in CapCut)
  const clips = [
    ['hello', { mood: 0 }, [700, 1600]],
    ['glowing', { mood: 3 }, []],
    ['scientist', { mood: 0, scientist: true }, []],
    ['sleepy', { mood: 6 }, []],
    ['asleep-nightcap', { mood: 5, nightcap: true }, []],
    ['reading', { mood: 4, glasses: true }, []],
    ['workout', { mood: 0, prop: 1, headband: true }, []],
    ['hearts', { mood: 0, prop: 2 }, []],
  ];
  for (const [name, inputs, taps] of (ONLY_GRAPHICS ? [] : clips)) {
    await clip(b, `bo-${name}.mp4`, inputs, SAGE, taps);
    await clip(b, `bo-${name}-greenscreen.mp4`, inputs, '#00FF00', taps);
  }
  fs.rmSync(path.join(out, 'tmp-video'), { recursive: true, force: true });
  await b.close();
  console.log('done');
})();
