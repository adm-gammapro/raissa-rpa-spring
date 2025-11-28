package com.raissa.rpa.util;

import org.openqa.selenium.WebElement;

import java.util.Random;

public abstract class MetodsGeneric {
    private MetodsGeneric() {}

    private static final Random random = new Random();

    /**
     * Genera un número aleatorio entre min y max (inclusivo)
     */
    private static int getRandomDelay(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Espera un tiempo aleatorio personalizado
     */
    public static void randomWait(int min, int max) throws InterruptedException {
        int delay = getRandomDelay(min, max);
        Thread.sleep(delay);
    }

    /**
     * Simula escritura humana con delays entre teclas
     */
    public static void humanTypeText(WebElement element, String text) throws InterruptedException {
        for (char c : text.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            Thread.sleep(getRandomDelay(100, 250));
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


}
