package com.raissa.rpa.service.bbva;

import java.util.Map;

public interface BBVAEmpresaService {
    /**
     * Metodo que autoriza ingreso a pagian del bbva
     *
     * @param credentials credenciales de acceso
     * @param transactionId id de transaccion
     * @return {@link Map} respuesta de logueo
     */
    Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId);

    /**
     * Obtiene datos de cuentas y saldos
     *
     * @param transactionId id de transaccion
     * @return {@link Map} Datos de cuentas y saldos
     */
    Map<String, Object> obtenerSaldo(String transactionId);

    /**
     * Obtiene datos de movimientos de una cuenta específica
     *
     * @param transactionId id de transaccion
     * @param numeroCuenta numero de cuenta
     * @param fechaInicio fecha de inicio para busqueda
     * @param fechaFin fecha de fin para busqueda
     * @param detalle indicador si se requiere extraer detalle
     * @return {@link Map} Datos de movimientos de cuenta
     */
    Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin, boolean detalle);

    /**
     * Obtiene datos de movimientos historicos de una cuenta específica
     *
     * @param transactionId id de transaccion
     * @param numeroCuenta numero de cuenta
     * @param fechaInicio fecha de inicio para busqueda
     * @param fechaFin fecha de fin para busqueda
     * @return {@link Map} Datos de movimientos de cuenta
     */
    Map<String, Object> obtenerMovimientosHistoricos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin);

    /**
     * Cierra sesion a la pagian del bbva
     *
     * @param transactionId id de transaccion
     * @return {@link Map} respuesta de logout
     */
    Map<String, Object> logout(String transactionId);
}
