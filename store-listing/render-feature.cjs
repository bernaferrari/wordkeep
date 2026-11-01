// Renders feature-graphic.html to en-US/feature-graphic.png (1024 × 500). Run from the repository root:
// node store-listing/render-feature.cjs
const path=require('path');
const studioPath=process.env.SCREENSHOT_STUDIO||path.join(require('os').homedir(),'Documents/screenshot-studio/studio/studio.cjs');
const {serve}=require(studioPath);
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||require.resolve('playwright',{paths:[process.cwd(),path.dirname(studioPath)]}));
(async()=>{const server=await serve(process.cwd());const browser=await chromium.launch({headless:true});try{const page=await browser.newPage({viewport:{width:1024,height:500},deviceScaleFactor:2});await page.goto(`http://127.0.0.1:${server.address().port}/store-listing/feature-graphic.html`);await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:'store-listing/en-US/feature-graphic.png',type:'png',scale:'css'});}finally{await browser.close();server.close();}})().catch(e=>{console.error(e);process.exitCode=1});
