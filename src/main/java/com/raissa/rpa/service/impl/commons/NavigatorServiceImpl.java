package com.raissa.rpa.service.impl.commons;

import com.microsoft.playwright.Page;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.config.PlaywrightManager;
import com.raissa.rpa.config.PlaywrightSession;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.commons.NavigatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavigatorServiceImpl implements NavigatorService {
    private final PlaywrightManager playwrightManager;
    private final Map<String, NavigatorSession> activeSessions = new ConcurrentHashMap<>();

    @Override
    public NavigatorSession iniciarNavegador(String transactionId, String bankCode) {
        try {
            log.info("Adquiriendo sesión para {} [tx={}]", bankCode, transactionId);

            // Usar el pool para adquirir una sesión
            NavigatorSession session = playwrightManager.acquireSession(bankCode, transactionId);

            // Verificar que la página esté activa
            Page page = session.page();
            if (page.isClosed()) {
                log.warn("La página estaba cerrada, liberando y adquiriendo nueva [tx={}]", transactionId);
                playwrightManager.releaseSession(bankCode, session, transactionId);
                session = playwrightManager.acquireSession(bankCode, transactionId);
                page = session.page();
            }

            // Configurar timeouts
            page.setDefaultTimeout(60000);
            page.setDefaultNavigationTimeout(60000);

            // Guardar en caché
            activeSessions.put(transactionId, session);

            log.info("Sesión adquirida exitosamente para {} [tx={}]", bankCode, transactionId);

            return session;

        } catch (Exception e) {
            log.error("Error al iniciar contexto de navegador: {}", e.getMessage(), e);
            throw new SessionNotFoundException("No se pudo iniciar el navegador: " + e.getMessage());
        }
    }

    /**
     * Libera una sesión para que sea reutilizada por otro usuario
     */
    public void liberarSesion(String transactionId, String bankCode, NavigatorSession session) {
        if (session != null) {
            try {
                playwrightManager.releaseSession(bankCode, session, transactionId);
                activeSessions.remove(transactionId);
                log.info("Sesión liberada para {} [tx={}]", bankCode, transactionId);
            } catch (Exception e) {
                log.error("Error liberando sesión: {}", e.getMessage());
            }
        }
    }
}