const puppeteer = require('puppeteer-core');

const wait = ms => new Promise(res => setTimeout(res, ms));

(async () => {
    const browser = await puppeteer.launch({ 
        headless: true,
        executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1536, height: 800 });

    try {
        console.log("Navigating to http://localhost:8080...");
        await page.goto('http://localhost:8080');
        await wait(2000); // let dashboard load API
        
        console.log("Saving preview_dashboard.png...");
        await page.screenshot({ path: 'preview_dashboard.png' });

        const tabs = ['products', 'customers', 'billing', 'history'];
        
        for (const tab of tabs) {
            console.log(`Clicking on ${tab}...`);
            await page.click(`button[data-page="${tab}"]`);
            await wait(1000); // let transition and API calls finish
            console.log(`Saving preview_${tab}.png...`);
            await page.screenshot({ path: `preview_${tab}.png` });
        }
        
    } catch (e) {
        console.error("Error capturing screenshots:", e);
    } finally {
        await browser.close();
        console.log("Done.");
    }
})();
