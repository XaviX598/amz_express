const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await page.goto('https://joyful-beijinho-263b20.netlify.app');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // Screenshot at top
    await page.screenshot({ path: 'C:/tmp/joyful-top.png' });
    console.log('Top screenshot saved');

    // 1. HERO — check for video or background image
    const heroInfo = await page.evaluate(() => {
        const header = document.querySelector('header');
        const sections = [...document.querySelectorAll('section, .hero, [class*="hero"]')];
        const bgElements = Array.from(document.querySelectorAll('*')).filter(el => {
            const style = window.getComputedStyle(el);
            return style.backgroundImage && style.backgroundImage !== 'none' && style.backgroundImage.includes('url');
        });
        const videos = Array.from(document.querySelectorAll('video'));
        return {
            sectionsCount: sections.length,
            heroClasses: sections.slice(0,3).map(s => s.className.substring(0, 100)),
            bgImagesCount: bgElements.length,
            bgImages: bgElements.slice(0,3).map(el => el.className.substring(0,80)),
            videosCount: videos.length,
            videoTags: videos.map(v => ({ src: v.src, autoplay: v.autoplay, loop: v.loop }))
        };
    });
    console.log('\n1. HERO:', JSON.stringify(heroInfo, null, 2));

    // 2. SLIDER — check for swiper/slider
    const sliderInfo = await page.evaluate(() => {
        const swipers = document.querySelectorAll('.swiper, [class*="swiper"], [class*="slider"]');
        const carousels = document.querySelectorAll('[class*="carousel"]');
        return {
            swiperCount: swipers.length,
            swiperClasses: Array.from(swipers).slice(0,5).map(s => s.className.substring(0,100)),
            carouselCount: carousels.length
        };
    });
    console.log('\n2. SLIDER:', JSON.stringify(sliderInfo, null, 2));

    // 3. PARALLAX — check for parallax styles and elements
    const parallaxInfo = await page.evaluate(() => {
        const parallax = Array.from(document.querySelectorAll('[class*="parallax"], [class*="fixed-bg"]'));
        const backgroundFixed = Array.from(document.querySelectorAll('*')).filter(el => {
            const style = window.getComputedStyle(el);
            return style.backgroundAttachment === 'fixed';
        });
        const transform3d = Array.from(document.querySelectorAll('*')).filter(el => {
            const style = window.getComputedStyle(el);
            return style.transform && style.transform.includes('3d') || el.style.transform?.includes('translate3d');
        });
        return {
            parallaxElements: parallax.slice(0,5).map(el => el.className.substring(0,80)),
            fixedBgCount: backgroundFixed.length,
            fixedBgClasses: backgroundFixed.slice(0,3).map(el => el.className.substring(0,80)),
            transform3dCount: transform3d.length
        };
    });
    console.log('\n3. PARALLAX:', JSON.stringify(parallaxInfo, null, 2));

    // 4. TYPOGRAPHY — check font families
    const fontInfo = await page.evaluate(() => {
        const fonts = new Set();
        for (const sheet of document.styleSheets) {
            try {
                for (const rule of sheet.cssRules) {
                    if (rule.cssText && rule.cssText.includes('font-family')) {
                        const match = rule.cssText.match(/font-family:\s*([^;]+)/);
                        if (match) fonts.add(match[1].trim());
                    }
                }
            } catch (e) {}
        }
        const headlines = Array.from(document.querySelectorAll('h1, h2')).slice(0,5).map(el => ({
            text: el.textContent?.trim().substring(0,50),
            fontSize: window.getComputedStyle(el).fontSize,
            fontWeight: window.getComputedStyle(el).fontWeight,
            fontFamily: window.getComputedStyle(el).fontFamily.substring(0,80)
        }));
        return { fonts: Array.from(fonts).slice(0,10), headlines };
    });
    console.log('\n4. TYPOGRAPHY:', JSON.stringify(fontInfo, null, 2));

    // 5. SCROLL STORYTELLING — check for section heights, scroll-based animations
    const scrollStorytelling = await page.evaluate(() => {
        const sections = Array.from(document.querySelectorAll('section, .page-section, [class*="section"]'));
        const tallSections = Array.from(sections).filter(el => {
            const h = el.getBoundingClientRect().height;
            return h > 500;
        });
        return {
            totalSections: sections.length,
            tallSectionClasses: tallSections.slice(0,5).map(s => ({ 
                class: s.className.substring(0,100), 
                height: Math.round(s.getBoundingClientRect().height)
            }))
        };
    });
    console.log('\n5. SCROLL STORYTELLING:', JSON.stringify(scrollStorytelling, null, 2));

    // 6. COMPOSITION — asymmetric layouts
    const composition = await page.evaluate(() => {
        const grids = Array.from(document.querySelectorAll('[class*="grid"], [class*="columns"], .content-header'));
        return {
            gridCount: grids.length,
            gridClasses: grids.slice(0,5).map(g => g.className.substring(0,100))
        };
    });
    console.log('\n6. ASYMMETRIC COMPOSITION:', JSON.stringify(composition, null, 2));

    // 7. CTA placement
    const ctaInfo = await page.evaluate(() => {
        const buttons = Array.from(document.querySelectorAll('a[href*="order"], a[href*="catering"], a[href*="menu"], .btn, [class*="btn"], [class*="cta"]'));
        return {
            ctaCount: buttons.length,
            ctaClasses: buttons.slice(0,5).map(b => ({ class: b.className.substring(0,80), href: b.href }))
        };
    });
    console.log('\n7. CTAs:', JSON.stringify(ctaInfo, null, 2));

    // Scroll through page
    await page.evaluate(() => window.scrollTo(0, 800));
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'C:/tmp/joyful-scroll1.png' });
    
    await page.evaluate(() => window.scrollTo(0, 2000));
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'C:/tmp/joyful-scroll2.png' });
    
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'C:/tmp/joyful-bottom.png' });
    console.log('\nScreenshots saved');

    // Check swiper JS initialization
    const swiperInit = await page.evaluate(() => {
        const scripts = Array.from(document.querySelectorAll('script[src]')).map(s => s.src);
        const swiperScripts = scripts.filter(s => s.includes('swiper'));
        return { totalScripts: scripts.length, swiperScripts };
    });
    console.log('\nSwiper scripts:', JSON.stringify(swiperInit, null, 2));

    await browser.close();
    console.log('\nDone!');
})();
