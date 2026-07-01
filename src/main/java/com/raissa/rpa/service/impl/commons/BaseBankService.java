package com.raissa.rpa.service.impl.commons;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.commons.NavigatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseBankService {
    protected final NavigatorService navigatorService;
    protected final ConcurrentMap<String, NavigatorSession> navigatorSessionCache = new ConcurrentHashMap<>();

    /**
     * Obtiene una sesión del caché
     */
    protected NavigatorSession getSession(String transactionId) {
        NavigatorSession session = navigatorSessionCache.get(transactionId);
        if (session == null) {
            throw new SessionNotFoundException("Sesión no encontrada para transactionId: " + transactionId);
        }
        return session;
    }

    /**
     * Guarda una sesión en el caché
     */
    protected void cacheSession(String transactionId, NavigatorSession session) {
        navigatorSessionCache.put(transactionId, session);
    }

    /**
     * Remueve una sesión del caché
     */
    protected void removeSessionFromCache(String transactionId) {
        navigatorSessionCache.remove(transactionId);
    }

    /**
     * Libera una sesión en el pool (ERROR - sin logout exitoso)
     */
    protected void releaseSessionOnError(String transactionId, String bankCode, NavigatorSession session) {
        if (session != null) {
            try {
                navigatorService.liberarSesion(transactionId, bankCode, session);
                navigatorSessionCache.remove(transactionId);
                log.info("Sesión liberada para {} debido a error [tx={}]", bankCode, transactionId);
            } catch (Exception e) {
                log.warn("Error al liberar sesión Playwright [tx={}]: {}", transactionId, e.getMessage());
            }
        }
    }

    /**
     * Libera una sesión en el pool (LOGOUT EXITOSO)
     */
    protected void releaseSessionOnLogout(String transactionId, String bankCode, NavigatorSession session) {
        if (session != null) {
            try {
                // Navegar a Google para limpiar estado
                try {
                    session.page().navigate("https://www.google.com", new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15_000));
                    log.info("Página navegada a Google para limpiar estado [tx={}]", transactionId);
                } catch (Exception e) {
                    log.warn("Error navegando a Google: {}", e.getMessage());
                }

                navigatorService.liberarSesion(transactionId, bankCode, session);
                navigatorSessionCache.remove(transactionId);
                log.info("Sesión liberada para {} [tx={}]", bankCode, transactionId);
            } catch (Exception e) {
                log.error("Error liberando sesión: {}", e.getMessage());
            }
        }
    }

    /**
     * Limpia una sesión (cierra recursos si es necesario)
     */
    protected void cleanupSession(NavigatorSession session) {
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("Error limpiando sesión: {}", e.getMessage());
            }
        }
    }

    /**
     * Navega a Google para limpiar la página
     */
    protected void navigateToGoogle(Page page) {
        try {
            if (page != null && !page.isClosed()) {
                page.navigate("https://www.google.com", new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)
                        .setTimeout(15_000));
            }
        } catch (Exception e) {
            log.warn("Error navegando a Google: {}", e.getMessage());
        }
    }
}
