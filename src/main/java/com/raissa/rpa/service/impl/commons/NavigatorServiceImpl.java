package com.raissa.rpa.service.impl.commons;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.config.PlaywrightManager;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.commons.NavigatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavigatorServiceImpl implements NavigatorService {
    private final PlaywrightManager playwrightManager;

    @Override
    public NavigatorSession iniciarNavegador(String transactionId) {
        BrowserContext context = null;

        try {
            context = playwrightManager.getBrowser().newContext(playwrightManager.defaultContextOptions());

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });"
            );
            Page page = context.newPage();

            log.info("Context/Page Playwright listos [tx={}]", transactionId);

            return new NavigatorSession(playwrightManager.getPlaywright(), context, page);
        } catch (Exception e) {
            log.error("Error al iniciar contexto de navegador: {}", e.getMessage(), e);
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
            throw new SessionNotFoundException("No se pudo iniciar el navegador: " + e.getMessage());
        }
    }
}