package com.raissa.rpa.config;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.UUID;

@Getter
@Slf4j
public class PlaywrightSession {
    private final Playwright playwright;
    private final BrowserContext context;
    private final Page page;
    private final String userDataDir;
    private final String sessionId;
    private boolean active = true;

    public PlaywrightSession(Playwright playwright, BrowserContext context, Page page, String userDataDir) {
        this.playwright = playwright;
        this.context = context;
        this.page = page;
        this.userDataDir = userDataDir;
        this.sessionId = UUID.randomUUID().toString();
    }

    public void close() {
        if (!active) return;

        try {
            if (page != null && !page.isClosed()) page.close();
        } catch (Exception e) {
            log.warn("Error cerrando page: {}", e.getMessage());
        }

        try {
            if (context != null) context.close();
        } catch (Exception e) {
            log.warn("Error cerrando context: {}", e.getMessage());
        }

        try {
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            log.warn("Error cerrando playwright: {}", e.getMessage());
        }

        // Limpiar directorio temporal
        try {
            FileUtils.deleteDirectory(new File(userDataDir));
        } catch (Exception e) {
            log.warn("Error limpiando directorio temporal: {}", e.getMessage());
        }

        active = false;
        log.info("Sesión Playwright cerrada: {}", sessionId);
    }
}
