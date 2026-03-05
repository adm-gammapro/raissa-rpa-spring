package com.raissa.rpa.service.bcp;

import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Map;

public interface BcpMenuService {
    /**
     * Verifica si el logueo fue exitoso
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean verifyLoginSuccess(Page page);

    /**
     * Metodo para detectar y cerrar el modal móvil si está abierto
     *
     * @param page manejador de pagina
     */
    void handleMobileModal(Page page);

    /**
     * Selecciona tab de cuentas
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean clickAccountsTab(Page page);

    /**
     * Verifica si se encuentra en el tab de cuentas
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean isOnAccountsPage(Page page);

    /**
     * Espera a que cargue la interfaz con las cuentas
     *
     * @param page manejador de página
     */
    void waitForAccountsToLoad(Page page);

    /**
     * Espera a que cargue la interfaz de resumen de cuentas
     *
     * @param page manejador de página
     */
    void waitForResumenAccountsToLoad(Page page);

    /**
     * Metodo de extracción para saldos de cuentas
     * @param page manejador de página
     *
     * @return {@link List<Map>} lista de saldos y cuentas
     */
    List<Map<String, Object>> extractAccountsData(Page page);

    /**
     * Hacer clic en la opción "Resumen" del menú lateral
     *
     * @param page manejador de página
     */
    void navigateToResumen(Page page);

    /**
     * Se ubica en una cuenta especifica de la lista de cuentas disponibles
     * @param page manejador de página
     * @param numeroCuenta numero de cuenta
     * @return {@link boolean}
     */
    boolean selectCuenta(Page page, String numeroCuenta);

    /**
     * Carga los valores de fechas en los inputs correspondientes de busqueda
     * @param page manejador de página
     * @param fechaInicio fecha inicial para la busqueda
     * @param fechaFin fecha final para la busqueda
     */
    void setDateRange(Page page, String fechaInicio, String fechaFin);

    /**
     * Presiona el boton de buscar para que realice la busqueda
     * @param page manejador de página
     */
    void applyFilters(Page page);

    /**
     * Espera que la tabla de movimientos cargue
     * @param page manejador de página
     */
    void waitForMovimientosToLoad(Page page);

    /**
     * Extrae la lista de movimientos de la pagina
     * @param page manejador de página
     * @return {@link List<Map>} Lista de movimientos
     */
    List<Map<String, Object>> extractMovimientosData(Page page);

    /**
     * Se ubica en una cuenta especifica de la lista de cuentas disponibles
     * @param page manejador de página
     * @param numeroCuenta numero de cuenta
     * @return {@link boolean}
     */
    boolean selectCuentaHistorico(Page page, String numeroCuenta);

    /**
     * Presiona el boton de buscar para que realice la busqueda
     * @param page manejador de página
     */
    void applyFiltersHistorico(Page page);

    /**
     * Abre el desplegable para cerrar sesion
     *
     * @param page manejador de pagina
     */
    void openProfileDropdown(Page page);

    /**
     * Hace clic en el boton de cerrar sesion
     *
     * @param page manejador de pagina
     */
    void clickLogoutButton(Page page);

    /**
     * Verifica si se abrio encuesta
     *
     * @param page manejador de pagina
     */
    void handleNpsSurvey(Page page);

    /**
     * Verifica que se haya cerrado la sesion
     *
     * @param page manejador de pagina
     * @return {@link boolean}
     */
    boolean verifyLogoutSuccess(Page page);

    /**
     * Verifica y hace clic en inicio de sesion
     * @param page manejador de pagina
     */
    void manejarModalSesionExpirada(Page page);
}
