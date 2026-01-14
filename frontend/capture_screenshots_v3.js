const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

(async () => {
    console.log('Launching browser...');
    const browser = await puppeteer.launch({
        headless: 'new',
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1400,900']
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1400, height: 900 });

    const baseUrl = 'https://shipflow.dev';
    const guidesDir = path.resolve(__dirname, 'public/guides');

    if (!fs.existsSync(guidesDir)) {
        fs.mkdirSync(guidesDir, { recursive: true });
    }

    // Helper to save screenshot
    const saveScreenshot = async (name) => {
        console.log(`Saving ${name}...`);
        await page.screenshot({ path: path.join(guidesDir, name) });
    };

    try {
        // 1. Login
        console.log('Navigating to Login...');
        await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });

        // Clear storage just in case
        await page.evaluate(() => localStorage.clear());

        await page.waitForSelector('#username', { timeout: 10000 });
        await page.type('#username', 'admin');
        await page.type('#password', 'admin123');
        await page.click('button[type="submit"]');
        console.log('Clicked login...');

        // Wait for Dashboard or Project Selector
        await page.waitForSelector('main', { timeout: 30000 });

        // Inject tour completion to be safe
        await page.evaluate(() => {
            localStorage.setItem('shipflow_welcome_shown', 'true');
            localStorage.setItem('shipflow_tour_completed', 'true');
        });

        // 2. Switch Project
        console.log('Switching Project to "Customer Portal"...');
        // The selector might be in a dropdown trigger. 
        // Look for the element with data-tour="project-selector" or just the button text
        try {
            // Open dropdown
            const projectSelectorBtn = await page.$('button[data-tour="project-selector"] button'); // The trigger is inside the div with data-tour?
            // Based on Layout.tsx: <div ... data-tour="project-selector"> <ProjectSelector /> </div>
            // ProjectSelector.tsx: <Button ... aria-label="Current project: ...">

            // Let's find the button by aria-label starts with "Current project" or "Select Project"
            const dropdownTrigger = await page.evaluateHandle(() => {
                const buttons = Array.from(document.querySelectorAll('button'));
                return buttons.find(b => b.getAttribute('aria-label') && b.getAttribute('aria-label').includes('Current project'));
            });

            if (dropdownTrigger && dropdownTrigger.asElement()) {
                await dropdownTrigger.asElement().click();
                await new Promise(r => setTimeout(r, 1000)); // Wait for animation

                // Find "Customer Portal" item
                const cpItem = await page.evaluateHandle(() => {
                    const items = Array.from(document.querySelectorAll('div[role="menuitem"]'));
                    return items.find(el => el.textContent.includes('Customer Portal'));
                });

                if (cpItem && cpItem.asElement()) {
                    await cpItem.asElement().click();
                    console.log('Selected Customer Portal.');
                    await new Promise(r => setTimeout(r, 2000)); // Wait for context switch request
                } else {
                    console.log('Customer Portal project not found in list, sticking with current default.');
                    // Maybe capture debug shot of menu
                    await page.screenshot({ path: path.join(guidesDir, 'project-menu-debug.png') });
                }
            } else {
                console.log('Could not find project selector trigger.');
            }

        } catch (err) {
            console.error('Error switching project:', err);
        }

        // 3. Capture Retro List (after switch)
        console.log('Capturing Retro List...');
        await page.goto(`${baseUrl}/retros`, { waitUntil: 'domcontentloaded' });
        await page.waitForSelector('h1', { timeout: 15000 });
        await new Promise(r => setTimeout(r, 2000));
        await saveScreenshot('retro-list.png');

        // 4. Capture Retro Board
        console.log('Capturing Retro Board...');
        // Try specific ID 1 as requested first
        await page.goto(`${baseUrl}/retros/1`, { waitUntil: 'domcontentloaded' });

        // Check if we got redirected to login or list
        const url = page.url();
        if (url.includes('/login')) {
            console.error('Redirected to login while accessing retro board! Re-logging in...');
            await page.type('#username', 'admin');
            await page.type('#password', 'admin123');
            await page.click('button[type="submit"]');
            await page.waitForNavigation();
        }

        // Wait for content
        try {
            await page.waitForSelector('h1', { timeout: 10000 }); // Title
            await new Promise(r => setTimeout(r, 3000));
            await saveScreenshot('retro-board.png');
        } catch (e) {
            console.log('Could not find header on retro board page, saving whatever is there.');
            await saveScreenshot('retro-board.png');
        }

    } catch (e) {
        console.error('Script failed:', e);
        await page.screenshot({ path: path.join(guidesDir, 'error-final.png') });
    } finally {
        await browser.close();
        console.log('Done.');
    }
})();
