package com.raissa.rpa.config;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record NavigatorSession(String transactionId,
                               PlaywrightSession playwrightSession) implements AutoCloseable {
    // Métodos de conveniencia para acceder a los recursos
    public Page page() {
        return playwrightSession.getPage();
    }

    public BrowserContext context() {
        return playwrightSession.getContext();
    }

    public Playwright playwright() {
        return playwrightSession.getPlaywright();
    }

    public boolean isActive() {
        return playwrightSession.isActive();
    }

    @Override
    public void close() {
        if (playwrightSession != null) {
            playwrightSession.close();
        }
        log.info("NavigatorSession cerrada [tx={}]", transactionId);
    }
}