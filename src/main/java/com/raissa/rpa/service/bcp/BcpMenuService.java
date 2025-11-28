package com.raissa.rpa.service.bcp;

import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

public interface BcpMenuService {
    /**
     * Verifica si el logueo fue exitoso
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    boolean verifyLoginSuccess(WebDriver driver);

    /**
     * Metodo para detectar y cerrar el modal móvil si está abierto
     *
     * @param driver manejador de pagina
     */
    void handleMobileModal(WebDriver driver);

    /**
     * Selecciona tab de cuentas
     *
     * @param driver manejador de página
     */
    void clickAccountsTab(WebDriver driver);

    /**
     * Verifica si se encuentra en el tab de cuentas
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    boolean isOnAccountsPage(WebDriver driver);

    /**
     * Espera a que cargue la interfaz con las cuentas
     *
     * @param driver manejador de página
     */
    void waitForAccountsToLoad(WebDriver driver);

    /**
     * Espera a que cargue la interfaz de resumen de cuentas
     *
     * @param driver manejador de página
     */
    void waitForResumenAccountsToLoad(WebDriver driver);

    /**
     * Metodo de extracción para saldos de cuentas
     * @param driver manejador de página
     *
     * @return {@link List<Map>} lista de saldos y cuentas
     */
    List<Map<String, Object>> extractAccountsData(WebDriver driver);

    /**
     * Hacer clic en la opción "Resumen" del menú lateral
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    boolean navigateToResumen(WebDriver driver);

    /**
     * Se ubica en una cuenta especifica de la lista de cuentas disponibles
     * @param driver manejador de página
     * @param numeroCuenta numero de cuenta
     * @return {@link boolean}
     */
    boolean selectCuenta(WebDriver driver, String numeroCuenta);

    /**
     * Carga los valores de fechas en los inputs correspondientes de busqueda
     * @param driver manejador de página
     * @param fechaInicio fecha inicial para la busqueda
     * @param fechaFin fecha final para la busqueda
     * @return {@link boolean}
     */
    boolean setDateRange(WebDriver driver, String fechaInicio, String fechaFin);

    /**
     * Presiona el boton de buscar para que realice la busqueda
     * @param driver manejador de página
     */
    void applyFilters(WebDriver driver);

    /**
     * Espera que la tabla de movimientos cargue
     * @param driver manejador de página
     */
    void waitForMovimientosToLoad(WebDriver driver);

    /**
     * Extrae la lista de movimientos de la pagina
     * @param driver manejador de página
     * @return {@link List<Map>} Lista de movimientos
     */
    List<Map<String, Object>> extractMovimientosData(WebDriver driver);

    /**
     * Se ubica en una cuenta especifica de la lista de cuentas disponibles
     * @param driver manejador de página
     * @param numeroCuenta numero de cuenta
     * @return {@link boolean}
     */
    boolean selectCuentaHistorico(WebDriver driver, String numeroCuenta);

    /**
     * Presiona el boton de buscar para que realice la busqueda
     * @param driver manejador de página
     */
    void applyFiltersHistorico(WebDriver driver);

    /**
     * Abre el desplegable para cerrar sesion
     *
     * @param driver manejador de pagina
     */
    void openProfileDropdown(WebDriver driver);

    /**
     * Hace clic en el boton de cerrar sesion
     *
     * @param driver manejador de pagina
     */
    void clickLogoutButton(WebDriver driver);

    /**
     * Verifica si se abrio encuesta
     *
     * @param driver manejador de pagina
     */
    void handleNpsSurvey(WebDriver driver);

    /**
     * Verifica que se haya cerrado la sesion
     *
     * @param driver manejador de pagina
     * @return {@link boolean}
     */
    boolean verifyLogoutSuccess(WebDriver driver);
}
