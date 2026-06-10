package com.raissa.rpa.service.bbva;

import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Map;

public interface BbvaMenuService {
    /**
     * Verifica si el menu esta visible
     *
     * @param page Manejador de pagina
     * @return {@link boolean}
     */
    boolean isMenuVisible(Page page);

    /**
     * Hace clic en el menu de posicion global
     *
     * @param page Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickCuentas(Page page);

    /**
     * Clic en posicion general online
     *
     * @param page Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickAllCuentas(Page page);

    /**
     * Extraccion de saldos
     *
     * @param page Manejador de pagina
     * @return {@link Map} datos de saldos
     */
    Map<String, Object> extractAccounts(Page page);

    /**
     * Realiza la bsuqueda por movimientos
     *
     * @param page Manejador de pagina
     * @param numeroCuenta Numeor de cuenta
     * @param fechaDesde fecha de inicio para busqueda
     * @param fechaHasta fecha de inicio para busqueda
     */
    void busquedaMovimientos(Page page, String numeroCuenta, String fechaDesde, String fechaHasta);

    /**
     * Extrae movimientos de la página
     *
     * @param page Manejador de pagina
     * @return {@link List<Map>}
     */
    List<Map<String, Object>> extraerMovimientos(Page page);

    /**
     * Extrae el detalle de los movimientos de la lista
     *
     * @param page Manejador de pagina
     * @param listMovements listado de movimientos
     * @return {@link List<Map>} Listado de movimientos actualizado
     */
    List<Map<String, Object>> extraerDetalleMovimientos(Page page, List<Map<String, Object>> listMovements);

    /**
     * Hace clic en el boton salir
     *
     * @param page Manejador de pagina
     * @return {@link boolean}
     */
    boolean clickSalir(Page page);

    /**
     * Clic al modal que cierra sesion
     * @param page manejador de pagina
     * @return {@link boolean}
     */
    boolean clickCerrarSesionModal(Page page);

    /**
     * Verifica si hay un modal y lo cierra
     *
     * @param page manejador de página
     */
    void closeModalIfPresent(Page page);
}
