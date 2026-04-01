const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await page.goto('https://joyful-beijinho-263b20.netlify.app');
    await page.waitForLoadState('networkidle');

    // Screenshot at top
    await page.screenshot({ path: 'C:/tmp/joyful-top.png' });
    console.log('Top screenshot saved');

    // Check navbar at top
    const navTop = await page.evaluate(() => {
        const nav = document.querySelector('nav');
        if (!nav) return null;
        const style = window.getComputedStyle(nav);
        const rect = nav.getBoundingClientRect();
        return {
            position: style.position,
            background: style.backgroundColor,
            backdropFilter: style.backdropFilter,
            boxShadow: style.boxShadow,
            borderBottom: style.borderBottom,
            height: style.height,
            padding: style.padding,
            rect: { top: rect.top, height: rect.height }
        };
    });
    console.log('\nNavbar at TOP:', JSON.stringify(navTop, null, 2));

    // Scroll to middle
    await page.evaluate(() => window.scrollTo(0, 600));
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'C:/tmp/joyful-scroll1.png' });
    console.log('\nScroll 600px screenshot saved');

    const navMiddle = await page.evaluate(() => {
        const nav = document.querySelector('nav');
        if (!nav) return null;
        const style = window.getComputedStyle(nav);
        return {
            position: style.position,
            background: style.backgroundColor,
            backdropFilter: style.backdropFilter,
            boxShadow: style.boxShadow,
            borderBottom: style.borderBottom,
            height: style.height,
            zIndex: style.zIndex
        };
    });
    console.log('\nNavbar at 600px scroll:', JSON.stringify(navMiddle, null, 2));

    // Check hero section
    const heroInfo = await page.evaluate(() => {
        const all = document.querySelectorAll('section, header, [class*="hero"], [class*="Hero"]');
        return Array.from(all).slice(0, 5).map(el => {
            const style = window.getComputedStyle(el);
            return {
                tag: el.tagName,
                class: el.className.substring(0, 200),
                minHeight: style.minHeight,
                height: style.height,
                position: style.position,
                zIndex: style.zIndex
            };
        });
    });
    console.log('\nHero sections:', JSON.stringify(heroInfo, null, 2));

    // Check fixed elements
    const fixedEls = await page.evaluate(() => {
        return Array.from(document.querySelectorAll('*')).filter(el => {
            const style = window.getComputedStyle(el);
            return style.position === 'fixed';
        }).map(el => ({
            tag: el.tagName,
            class: el.className.substring(0, 150),
            zIndex: window.getComputedStyle(el).zIndex
        }));
    });
    console.log('\nFixed elements:', JSON.stringify(fixedEls.slice(0, 10), null, 2));

    // Check for scroll event listeners
    const scrollListeners = await page.evaluate(() => {
        // Try to detect scroll-based classes
        const all = Array.from(document.querySelectorAll('[class]')).filter(el => 
            el.className.includes('scroll') || 
            el.className.includes('sticky') || 
            el.className.includes('fixed') ||
            el.className.includes('nav')
        );
        return all.slice(0, 10).map(el => ({
            tag: el.tagName,
            class: el.className.substring(0, 200)
        }));
    });
    console.log('\nScroll/sticky/fixed related classes:', JSON.stringify(scrollListeners, null, 2));

    // Scroll to bottom
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'C:/tmp/joyful-bottom.png' });

    const navBottom = await page.evaluate(() => {
        const nav = document.querySelector('nav');
        if (!nav) return null;
        const style = window.getComputedStyle(nav);
        return {
            position: style.position,
            background: style.backgroundColor,
            backdropFilter: style.backdropFilter,
            boxShadow: style.boxShadow,
            borderBottom: style.borderBottom
        };
    });
    console.log('\nNavbar at bottom:', JSON.stringify(navBottom, null, 2));

    await browser.close();
    console.log('\nDone!');
})();
