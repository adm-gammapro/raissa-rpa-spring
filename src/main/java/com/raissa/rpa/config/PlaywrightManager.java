package com.raissa.rpa.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ColorScheme;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PlaywrightManager {
    @Value("${app.isProduction:false}")
    private boolean isProduction;

    @Value("${app.ua.prod:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36}")
    private String uaProd;

    @Value("${app.ua.dev:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36}")
    private String uaDev;

    @Getter
    private Playwright playwright;

    @Getter
    private Browser browser;

    @PostConstruct
    public void init() {
        playwright = Playwright.create();

        List<String> args = isProduction
                ? List.of(
                "--disable-blink-features=AutomationControlled",
                "--lang=es-PE",
                "--accept-lang=es-PE,es;q=0.9",
                "--disable-features=IsolateOrigins,site-per-process",
                "--font-render-hinting=medium",
                "--no-sandbox",
                "--disable-dev-shm-usage"
        )
                : List.of(
                "--disable-blink-features=AutomationControlled",
                "--lang=es-PE",
                "--accept-lang=es-PE,es;q=0.9",
                "--disable-features=IsolateOrigins,site-per-process",
                "--font-render-hinting=medium"
        );

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(isProduction)
                .setArgs(args);

        browser = playwright.chromium().launch(launchOptions);

        log.info("Playwright/Brower inicializado correctamente [produccion={}]", isProduction);
    }

    public Browser.NewContextOptions defaultContextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(isProduction ? 1920 : 1680, isProduction ? 1080 : 1000)
                .setUserAgent(isProduction ? uaProd : uaDev)
                .setLocale("es-PE")
                .setTimezoneId("America/Lima")
                .setColorScheme(ColorScheme.LIGHT)
                .setIgnoreHTTPSErrors(true);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (browser != null) browser.close();
        } catch (Exception e) {
            log.warn("Error cerrando browser: {}", e.getMessage());
        }
        try {
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            log.warn("Error cerrando playwright: {}", e.getMessage());
        }
        log.info("PlaywrightManager finalizado");
    }
}
