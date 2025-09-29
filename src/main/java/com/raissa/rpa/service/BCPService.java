package com.raissa.rpa.service;

import java.util.Map;

public interface BCPService {
    /**
     * Logueo en la pagina del BCP
     * @param credentials credenciales de acceso al banco
     * @param transactionId id de transaccion
     * @return {@link Map} resultado de logueo
     */
    Map<String, Object> login(Map<String, String> credentials, String transactionId);

    /**
     * Obtiene datos de cuentas
     * @param transactionId id de transaccion
     * @return {@link Map} retorna lista con datos de cuentas
     */
    Map<String, Object> obtenerSaldo(String transactionId);

    //String obtenerTransacciones(String fechaInicio, String fechaFin);

    /**
     * Logout de BCP
     *
     * @param transactionId id de transaccion
     * @return {@link Map} mensaje de confirmacion
     */
    Map<String, Object> logout(String transactionId);
}
