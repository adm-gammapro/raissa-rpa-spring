package com.raissa.rpa.util;

import com.microsoft.playwright.Keyboard;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.raissa.rpa.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public abstract class MetodsGeneric {
    private MetodsGeneric() {}

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * Genera un número aleatorio entre min y max (inclusivo)
     */
    private static int getRandomDelay(int min, int max) {
        return random.nextInt(min, max + 1);
    }

    /**
     * Espera un tiempo aleatorio personalizado, pero duerme la aplicacion
     */
    public static void randomWait(int min, int max) throws InterruptedException {
        int delay = getRandomDelay(min, max);
        Thread.sleep(delay);
    }

    /**
     * Espera un tiempo aleatorio personalizado, pero pone en espera a la pagina
     */
    public static void randomWaitPage(Page page, int min, int max) {
        int delay = getRandomDelay(min, max);
        page.waitForTimeout(delay);
    }

    /**
     * Simula escritura humana con delays entre teclas
     */
    public static void humanTypeText(Locator locator, String text, int minDelayMs, int maxDelayMs) {
        for (char c : text.toCharArray()) {
            locator.type(String.valueOf(c), new Locator.TypeOptions().setDelay(getRandomDelay(minDelayMs, maxDelayMs)));
            try {
                Thread.sleep(getRandomDelay(60, 120)); // pequeña pausa adicional
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new ValidationException("Interrumpido mientras se tipeaba", ie);
            }
        }
    }

    /**
     * Simula escritura humana con delays entre teclas no especifico en objeto, sino sobre la pagina
     */
    public static void humanTypeWithKeyboard(Page page, String text, int minDelayMs, int maxDelayMs) {
        if (text == null) return;
        for (char c : text.toCharArray()) {
            page.keyboard().type(String.valueOf(c), new Keyboard.TypeOptions()
                    .setDelay(ThreadLocalRandom.current().nextInt(minDelayMs, maxDelayMs + 1)));
            MetodsGeneric.randomWaitPage(page, 60, 120);
        }
    }

    /**
     * Formatea el monto mostrado en la pagina a numero
     * @param saldoStr Saldo en texto
     * @return {@link double} saldo en formato numerico
     */
    public static double parseSaldo(String saldoStr) {
        if (saldoStr == null || saldoStr.trim().isEmpty()) {
            return 0.0;
        }

        try {
            String cleaned = saldoStr
                    .replace("S/ ", "")
                    .replace("$ ", "")
                    .replace("S/", "")
                    .replace("$", "")
                    .trim();

            cleaned = cleaned
                    .replaceAll("[^\\d.,-]", "")
                    .replace(",", "")
                    .trim();

            if (cleaned.isEmpty() || cleaned.equals("-")) {
                return 0.0;
            }

            boolean esNegativo = cleaned.startsWith("-");
            if (esNegativo) {
                cleaned = cleaned.substring(1);
            }

            double resultado = Double.parseDouble(cleaned);

            return esNegativo ? -resultado : resultado;

        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Quitar caracteres especiales del número de cuenta
     *
     * @param numeroCuenta numero de cuenta
     * @return {@link String} numero de cuenta sin caracteres especiales
     */
    public static String cleanAccountNumber(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isEmpty()) {
            return "";
        }

        // Quitar espacios, guiones, puntos, y otros caracteres no numéricos
        return numeroCuenta.replaceAll("[\\s\\-._]+", "");
    }

    /**
     * Completa un número a 10 dígitos rellenando con ceros a la izquierda
     *
     * @param numeroText El texto del número a completar
     * @return String con 10 dígitos, o ceros si hay error
     */
    public static String completarADiezDigitos(String numeroText) {
        try {
            if (numeroText == null || numeroText.trim().isEmpty()) {
                return Constantes.VALOR_CEROS;
            }

            String numeroLimpio = numeroText.replaceAll("[^0-9]", "");

            if (numeroLimpio.isEmpty()) {
                return Constantes.VALOR_CEROS;
            }

            long numero;
            try {
                numero = Long.parseLong(numeroLimpio);
            } catch (NumberFormatException e) {
                if (numeroLimpio.length() > 10) {
                    return numeroLimpio.substring(numeroLimpio.length() - 10);
                } else {
                    return Constantes.VALOR_CEROS;
                }
            }

            if (numeroLimpio.length() < 10) {
                return String.format("%010d", numero);
            } else if (numeroLimpio.length() > 10) {
                return numeroLimpio.substring(numeroLimpio.length() - 10);
            } else {
                return numeroLimpio;
            }

        } catch (Exception e) {
            return Constantes.VALOR_CEROS;
        }
    }

    /**
     * Metodo que verifica si un elemento es visible y espera un tiempo determinado su carga
     * @param page Manejador de pagina
     * @param selector Elemento de pagina
     * @param timeoutMs Tiempo de espera
     * @return {@link Locator} Elemento encontrado
     */
    public static Locator waitForVisible(Page page, String selector, int timeoutMs) {
        Locator locator = page.locator(selector).first();
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));
        return locator;
    }

    /**
     * Metodo que verifica si un elemento existe y espera un tiempo determinado su carga
     * @param page Manejador de pagina
     * @param selector Elemento de pagina
     * @param timeoutMs Tiempo de espera
     * @return {@link Locator} Elemento encontrado
     */
    public static Locator waitForAttached(Page page, String selector, int timeoutMs) {
        Locator locator = page.locator(selector).first();
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.ATTACHED)
                .setTimeout(timeoutMs));
        return locator;
    }

    /**
     * Metodo que verifica si un elemento existe y espera un tiempo determinado su carga
     * @param locator Elemento de pagina
     * @param timeoutMs Tiempo de espera
     */
    public static void waitForHidden(Locator locator, int timeoutMs) {
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs));
    }

    /**
     * Toma un screenshoot de la pantalla del navegador
     *
     * @param page Manejador de pagina
     * @param tag nombre
     * @param transactionId codigo de transaccion
     */
    public static void debugSnapshot(Page page, String tag, String transactionId) {
        try {
            long ts = System.currentTimeMillis();

            log.info("[{}] URL: {}", tag, page.url());
            log.info("[{}] TITLE: {}", tag, page.title());
            log.info("[{}] FRAMES: {}", tag, page.frames().size());

            String png = "/opt/rpa/err/bcp_" + tag + "_" + transactionId + "_" + ts + ".png";
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(png))
                    .setFullPage(true));
            log.info("[{}] Screenshot: {}", tag, png);

            String html = "/opt/rpa/err/bcp_" + tag + "_" + transactionId + "_" + ts + ".html";
            Files.writeString(Paths.get(html), page.content());
            log.info("[{}] HTML: {}", tag, html);
        } catch (Exception ex) {
            log.warn("[{}] Error en debugSnapshot: {}", tag, ex.getMessage());
        }
    }

    /**
     * Hace clic en un elemento de la pagina
     * @param page Manejador de pagina
     * @param loc elemento
     * @param timeoutMs tiempo de espera
     * @return {@link boolean}
     */
    public static boolean clickWithFallback(Page page, Locator loc, int timeoutMs) {
        try {
            loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
            MetodsGeneric.randomWaitPage(page, 150, 200);

            return true;
        } catch (Exception e) {
            log.debug("Click directo falló, probando JS click: {}", e.getMessage());
            loc.evaluate("el => el.click()");
            return false;
        }
    }

    /**
     * Fuerza clic en un elemento de la pagina
     * @param page Manejador de pagina
     * @param loc elemento
     */
    public static void clickJsFallback(Page page, Locator loc) {
        try {
            loc.evaluate("el => el.click()");
            MetodsGeneric.randomWaitPage(page, 150, 200);
        } catch (Exception e) {
            log.debug("Force click falló, probando JS click: {}", e.getMessage());
        }
    }
}