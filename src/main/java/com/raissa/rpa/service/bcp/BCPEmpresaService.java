package com.raissa.rpa.service.bcp;

import java.util.Map;

public interface BCPEmpresaService {
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

    /**
     * Obtener transacciones por periodo
     *
     * @param transactionId id de transaccion
     * @param numeroCuenta numero de cuenta
     * @param fechaInicio fecha inicial de busqueda
     * @param fechaFin fecha final de busqueda
     * @return {@link Map} datos de movimientos
     */
    Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin);

    /**
     * Logout de BCP
     *
     * @param transactionId id de transaccion
     * @return {@link Map} mensaje de confirmacion
     */
    Map<String, Object> logout(String transactionId);
}
