package com.raissa.rpa.service.bbva;

import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

public interface BbvaMenuService {
    /**
     * Verifica si el menu esta visible
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean isMenuVisible(WebDriver driver);

    /**
     * Hace clic en el menu de posicion global
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickCuentas(WebDriver driver);

    /**
     * Clic en posicion general online
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickPosicionGlobalOnline(WebDriver driver);

    /**
     * Extraccion de saldos
     *
     * @param driver Manejador de pagina
     * @return {@link Map} datos de saldos
     */
    Map<String, Object> extractAccounts(WebDriver driver);

    /**
     * Clic en movimientos
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickMovimientos(WebDriver driver);

    /**
     * Realiza la bsuqueda por movimientos
     *
     * @param driver Manejador de pagina
     * @param numeroCuenta Numeor de cuenta
     * @param fechaDesde fecha de inicio para busqueda
     * @param fechaHasta fecha de inicio para busqueda
     */
    void busquedaMovimientos(WebDriver driver, String numeroCuenta, String fechaDesde, String fechaHasta);

    /**
     * Extrae movimientos de la página
     *
     * @param driver Manejador de pagina
     * @return {@link List<Map>}
     */
    List<Map<String, Object>> extraerMovimientos(WebDriver driver);

    /**
     * Extrae el detalle de los movimientos de la lista
     *
     * @param driver Manejador de pagina
     * @param listMovements listado de movimientos
     * @return {@link List<Map>} Listado de movimientos actualizado
     */
    List<Map<String, Object>> extraerDetalleMovimientos(WebDriver driver, List<Map<String, Object>> listMovements);

    /**
     * Clic en movimientos historicos
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickMovimientosHistoricos(WebDriver driver);

    /**
     * Realiza la busqueda por movimientos historico
     *
     * @param driver Manejador de pagina
     * @param numeroCuenta Número de cuenta
     * @param fechaDesde fecha de inicio para busqueda
     * @param fechaHasta fecha de inicio para busqueda
     */
    void busquedaMovimientosHistoricos(WebDriver driver, String numeroCuenta, String fechaDesde, String fechaHasta);

    /**
     * Extrae movimientos historicos de la página
     *
     * @param driver Manejador de pagina
     * @return {@link List<Map>}
     */
    List<Map<String, Object>> extraerMovimientosHistoricos(WebDriver driver);

    /**
     * Hace clic en el boton salir
     *
     * @param driver Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickSalir(WebDriver driver);
}
