import { chromium } from 'playwright';

const BASE = 'http://localhost:8002';
const out = 'c:/Users/Ximenez2021/Desktop/subject1/anime-system/scripts/detail_shot.png';

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
page.on('console', m => { if (m.type() === 'error') console.log('PAGE ERR:', m.text()); });

await page.goto(BASE + '/#/login', { waitUntil: 'networkidle' });
await page.waitForTimeout(800);
await page.fill('input[type="text"]', 'admin');
await page.fill('input[type="password"]', '123456');
await page.click('button[type="submit"]');
await page.waitForTimeout(1500);
console.log('after login url:', page.url());

await page.goto(BASE + '/#/video/details?id=34465', { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);
await page.screenshot({ path: out, fullPage: true });
console.log('screenshot saved:', out);

await browser.close();
