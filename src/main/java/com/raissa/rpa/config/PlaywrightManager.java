package com.raissa.rpa.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.raissa.rpa.exception.SessionNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Slf4j
@Component
public class PlaywrightManager {
    // Configuración por banco
    private static final int POOL_SIZE = 3; // Ventanas por banco
    private static final int BCP_BASE_PORT = 9222;
    private static final int INTERBANK_BASE_PORT = 9230; // Cambio: 9230 en adelante
    private static final int BBVA_BASE_PORT = 9238;      // Cambio: 9238 en adelante

    private final Map<String, Queue<NavigatorSession>> availableSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<NavigatorSession>> inUseSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Inicializando PlaywrightManager con pool de {} ventanas por banco", POOL_SIZE);

        inicializarPool("BCP", BCP_BASE_PORT);
        inicializarPool("INTERBANK", INTERBANK_BASE_PORT);
        inicializarPool("BBVA", BBVA_BASE_PORT);

        log.info("PlaywrightManager inicializado correctamente");
    }

    private void inicializarPool(String bankCode, int basePort) {
        Queue<NavigatorSession> available = new ConcurrentLinkedQueue<>();
        Set<NavigatorSession> inUse = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < POOL_SIZE; i++) {
            int port = basePort + i;
            String sessionId = "pool-" + bankCode.toLowerCase() + "-" + i;

            try {
                log.info("Creando ventana {} para {} en puerto {}", i, bankCode, port);

                // Crear la sesión
                PlaywrightSession playwrightSession = connectToBrowser(sessionId, port, bankCode);
                NavigatorSession session = new NavigatorSession(sessionId, playwrightSession);

                // Dejar la página en Google (limpia)
                session.page().navigate("https://www.google.com", new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.NETWORKIDLE)
                        .setTimeout(30_000));

                available.offer(session);

            } catch (Exception e) {
                log.error("Error creando ventana {} para {}: {}", i, bankCode, e.getMessage());
            }
        }

        availableSessions.put(bankCode, available);
        inUseSessions.put(bankCode, inUse);

        log.info("Pool para {} inicializado con {} ventanas disponibles", bankCode, available.size());
    }

    /**
     * Adquiere una sesión disponible para el banco
     */
    public synchronized NavigatorSession acquireSession(String bankCode, String transactionId) {
        Queue<NavigatorSession> available = availableSessions.get(bankCode);
        Set<NavigatorSession> inUse = inUseSessions.get(bankCode);

        if (available == null || inUse == null) {
            throw new SessionNotFoundException("Pool no inicializado para " + bankCode);
        }

        NavigatorSession session = available.poll();

        if (session == null) {
            throw new SessionNotFoundException("No hay ventanas disponibles para " + bankCode +
                    ". Límite: " + POOL_SIZE + " concurrentes.");
        }

        // Verificar que la sesión sea válida
        if (!isSessionValid(session)) {
            log.warn("Sesión inválida para {}, recreando...", bankCode);
            session = recreateSession(bankCode, transactionId);
        }

        inUse.add(session);
        log.info("Sesión adquirida para {} [tx={}], disponibles: {}, en uso: {}",
                bankCode, transactionId, available.size(), inUse.size());

        return session;
    }

    /**
     * Libera una sesión para que pueda ser reutilizada
     */
    public synchronized void releaseSession(String bankCode, NavigatorSession session, String transactionId) {
        Queue<NavigatorSession> available = availableSessions.get(bankCode);
        Set<NavigatorSession> inUse = inUseSessions.get(bankCode);

        if (available == null || inUse == null) {
            log.warn("Pool no encontrado para {}", bankCode);
            return;
        }

        if (!inUse.contains(session)) {
            log.warn("Sesión no estaba en uso para {}", bankCode);
            return;
        }

        // Limpiar la sesión
        try {
            session.page().navigate("https://www.google.com", new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(30_000));
        } catch (Exception e) {
            log.warn("Error limpiando sesión para {}: {}", bankCode, e.getMessage());
        }

        inUse.remove(session);
        available.offer(session);

        log.info("Sesión liberada para {} [tx={}], disponibles: {}, en uso: {}",
                bankCode, transactionId, available.size(), inUse.size());
    }

    /**
     * Verifica si una sesión es válida
     */
    private boolean isSessionValid(NavigatorSession session) {
        try {
            if (session == null || !session.isActive()) return false;
            Page page = session.page();
            if (page.isClosed()) return false;
            page.evaluate("() => 1 + 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recrea una sesión que ha fallado
     */
    private NavigatorSession recreateSession(String bankCode, String transactionId) {
        int port = getBasePort(bankCode); // Simplificado: usa el primer puerto

        PlaywrightSession playwrightSession = connectToBrowser(
                "recreated-" + bankCode + "-" + System.currentTimeMillis(),
                port,
                bankCode
        );

        return new NavigatorSession(transactionId, playwrightSession);
    }

    private int getBasePort(String bankCode) {
        switch (bankCode.toUpperCase()) {
            case "BCP": return BCP_BASE_PORT;
            case "INTERBANK": return INTERBANK_BASE_PORT;
            case "BBVA": return BBVA_BASE_PORT;
            default: throw new IllegalArgumentException("Banco no soportado: " + bankCode);
        }
    }

    /*public PlaywrightSession createBcpSession(String transactionId) {
        return connectToBrowser(transactionId, BCP_PORT, "BCP");
    }

    public PlaywrightSession createInterbankSession(String transactionId) {
        return connectToBrowser(transactionId, INTERBANK_PORT, "Interbank");
    }

    public PlaywrightSession createBbvaSession(String transactionId) {
        return connectToBrowser(transactionId, BBVA_PORT, "BBVA");
    }*/

    private PlaywrightSession connectToBrowser(String transactionId, int port, String bankName) {
        Playwright playwright = null;
        Browser browser;

        try {
            log.info("Conectando a {} en puerto {} [tx={}]", bankName, port, transactionId);
            playwright = Playwright.create();

            String cdpEndpoint = String.format("http://localhost:%d", port);
            browser = playwright.chromium().connectOverCDP(cdpEndpoint);

            BrowserContext context = browser.contexts().get(0);
            Page page = context.pages().get(0);
            page.setDefaultTimeout(60000);

            log.info("Conexión exitosa a {} [tx={}]", bankName, transactionId);

            return new PlaywrightSession(playwright, context, page, null);

        } catch (Exception e) {
            if (playwright != null) playwright.close();
            throw new SessionNotFoundException(String.format(
                    "Error conectando a %s. Asegúrate de que Chrome esté abierto con --remote-debugging-port=%d",
                    bankName, port));
        }
    }

    public Map<String, Map<String, Integer>> getPoolStats() {
        Map<String, Map<String, Integer>> stats = new HashMap<>();
        for (String bankCode : availableSessions.keySet()) {
            Map<String, Integer> bankStats = new HashMap<>();
            bankStats.put("available", availableSessions.getOrDefault(bankCode, new ConcurrentLinkedQueue<>()).size());
            bankStats.put("inUse", inUseSessions.getOrDefault(bankCode, ConcurrentHashMap.newKeySet()).size());
            bankStats.put("total", POOL_SIZE);
            stats.put(bankCode, bankStats);
        }
        return stats;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Cerrando PlaywrightManager...");

        for (Queue<NavigatorSession> sessions : availableSessions.values()) {
            for (NavigatorSession session : sessions) {
                try { session.close(); } catch (Exception e) { log.warn("Error: {}", e.getMessage()); }
            }
        }

        for (Set<NavigatorSession> sessions : inUseSessions.values()) {
            for (NavigatorSession session : sessions) {
                try { session.close(); } catch (Exception e) { log.warn("Error: {}", e.getMessage()); }
            }
        }

        log.info("PlaywrightManager cerrado");
    }
}
