package com.raissa.rpa.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class ResponseGeneric {
    /**
     * Mapea una respuesta generica
     *
     * @param transactionId id Transacción
     * @param message mensaje de respuesta
     * @param success estado de la respuesta
     *
     * @return {@link Map} devuelve un response genérico
     */
    public static Map<String, Object> buildSuccessResponse(String transactionId, String message, boolean success) {
        Map<String, Object> response = new HashMap<>();
        response.put(Constantes.KEY_SUCCESS, success);
        response.put(Constantes.KEY_MESSAGE, message);
        response.put(Constantes.KEY_TRANSACTION_ID, transactionId);
        return response;
    }

    /**
     * Obtiene ip del quequest de la peticion
     *
     * @param request peticion
     * @return {@link String} ip obtenida
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Genera transactionId único
     *
     * @return {@link String}
     */
    public static String generateTransactionId(String prefijoTransaccion) {
        Random random = new Random();

        return prefijoTransaccion + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
}
