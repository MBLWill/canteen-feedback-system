// 通过 Chrome DevTools Protocol 登录并截图小助手，验证浮动面板与圆球停靠效果。
// 用法：node scripts/cdp-shot.cjs
// 依赖：Node 22+ 内置 WebSocket，无需额外安装
const http = require('http');
const fs = require('fs');
const path = require('path');

const CDP = 'http://127.0.0.1:9222';
const OUT = path.join(process.env.TEMP || '/tmp', 'widget-shots');
fs.mkdirSync(OUT, { recursive: true });

const get = (url) => new Promise((res, rej) => {
  http.get(url, r => {
    let d = '';
    r.on('data', c => (d += c));
    r.on('end', () => res(JSON.parse(d)));
  }).on('error', rej);
});

const sleep = ms => new Promise(r => setTimeout(r, ms));

(async () => {
  const targets = await get(CDP + '/json/list');
  const page = targets.find(t => t.type === 'page' && t.url.includes('localhost:5173'));
  if (!page) throw new Error('找不到 localhost:5173 页面，请先打开');

  // Node 内置 WebSocket 使用 addEventListener
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();

  ws.addEventListener('message', ev => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { res, rej } = pending.get(msg.id);
      pending.delete(msg.id);
      if (msg.error) rej(new Error(msg.error.message));
      else res(msg.result);
    }
  });

  await new Promise((res, rej) => {
    ws.addEventListener('open', res, { once: true });
    ws.addEventListener('error', () => rej(new Error('WebSocket 连接失败')), { once: true });
  });

  const send = (method, params = {}) => new Promise((res, rej) => {
    const mid = ++id;
    pending.set(mid, { res, rej });
    ws.send(JSON.stringify({ id: mid, method, params }));
  });

  const evaluate = async (expr) => {
    const r = await send('Runtime.evaluate', {
      expression: expr, awaitPromise: true, returnByValue: true
    });
    if (r.exceptionDetails) {
      throw new Error(r.exceptionDetails.text + ' ' +
        (r.exceptionDetails.exception?.description || ''));
    }
    return r.result.value;
  };

  await send('Page.enable');
  await send('Runtime.enable');

  // 1) 登录
  await send('Page.navigate', { url: 'http://localhost:5173/login' });
  await sleep(2500);

  const loginState = await evaluate(`
    (() => {
      const u = document.querySelector('input[type="text"], input:not([type])');
      const p = document.querySelector('input[type="password"]');
      if (!u || !p) return 'no-input';
      const setVal = (el, v) => {
        const setter = Object.getOwnPropertyDescriptor(el.constructor.prototype, 'value').set;
        setter.call(el, v);
        el.dispatchEvent(new Event('input', { bubbles: true }));
      };
      setVal(u, 'student01');
      setVal(p, '123456');
      const btn = document.querySelector('button.el-button--primary, button[type="submit"]');
      if (btn) btn.click();
      return 'ok';
    })()
  `);
  console.log('登录操作:', loginState);
  await sleep(3200);
  console.log('登录后 URL:', await evaluate('location.href'));

  // 2) 记录圆球收起时的位置
  const fabBefore = await evaluate(`
    (() => {
      const f = document.querySelector('.chat-fab');
      if (!f) return null;
      const r = f.getBoundingClientRect();
      return { left: Math.round(r.left), top: Math.round(r.top) };
    })()
  `);
  console.log('圆球收起位置:', JSON.stringify(fabBefore));

  // 3) 点开小助手
  await evaluate(`
    (() => {
      const f = document.querySelector('.chat-fab');
      if (f) f.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, button: 0 }));
      window.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, button: 0 }));
      return 'clicked';
    })()
  `);
  await sleep(1000);

  // 4) 面板与圆球位置
  const geo = await evaluate(`
    (() => {
      const p = document.querySelector('.chat-panel');
      const f = document.querySelector('.chat-fab');
      if (!p || !f) return null;
      const pr = p.getBoundingClientRect();
      const fr = f.getBoundingClientRect();
      return {
        panel: { left: Math.round(pr.left), top: Math.round(pr.top),
                 w: Math.round(pr.width), h: Math.round(pr.height),
                 right: Math.round(pr.right), bottom: Math.round(pr.bottom) },
        fab: { left: Math.round(fr.left), top: Math.round(fr.top),
               cx: Math.round(fr.left + fr.width/2), cy: Math.round(fr.top + fr.height/2) },
        vp: { w: window.innerWidth, h: window.innerHeight }
      };
    })()
  `);
  console.log('几何信息:', JSON.stringify(geo, null, 2));

  let shot = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(path.join(OUT, '01-open.png'), Buffer.from(shot.data, 'base64'));
  console.log('截图:', path.join(OUT, '01-open.png'));

  ws.close();
})().catch(e => { console.error('失败:', e.message); process.exit(1); });
