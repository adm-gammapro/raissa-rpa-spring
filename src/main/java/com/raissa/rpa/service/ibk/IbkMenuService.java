package com.raissa.rpa.service.ibk;

import com.microsoft.playwright.Page;

import java.util.List;
import java.util.Map;

public interface IbkMenuService {
    /**
     * Verifica si el logueo fue exitoso
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean verifyLoginSuccess(Page page);

    /**
     * @param page manejador de página
     *
     * @return {@link boolean}
     */
    boolean closeCampaignPopupIfPresent(Page page);

    /**
     * Hace clic en la opcion consultas saldos
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean clickConsultas(Page page);

    /**
     * Extrae los datos de las cuentas
     *
     * @param page manejador de pagina
     * @return {@link List}
     */
    List<Map<String, Object>> extractAccountsData(Page page);

    /**
     * Hace clic en la opcion de movimientos
     * @param page manejador de página
     * @return {@link boolean}
     */
    boolean clickMovimientos(Page page);

    /**
     * Selecciona una cuenta para extraccion
     *
     * @param page manejador de pagina
     * @param numeroCuenta numero de cuenta a extraer movimientos
     * @return {@link boolean}
     */
    boolean selectCuenta(Page page, String numeroCuenta);

    /**
     * Carga las fechas en la busqueda
     *
     * @param page manejador de pagina
     * @param fechaInicio valro de fecha inicial de busqueda
     * @param fechaFin valor de fecha final de busqueda
     * @return {@link boolean}
     */
    boolean setDateRange(Page page, String fechaInicio, String fechaFin);

    /**
     * Aplica los filtros de fechas cargados
     *
     * @param page maenjador de pagina
     */
    void applyFilters(Page page);

    /**
     * Espera a que los movimientos hayan cargado en la pagina
     *
     * @param page manejador de pagina
     * @return {@link boolean}
     */
    boolean waitForMovimientosToLoad(Page page);

    /**
     * Extrae los movimientos de la pagina
     *
     * @param page manejador de pagina
     * @return {@link List}
     */
    List<Map<String, Object>> extractMovimientosData(Page page);

    /**
     * Abre dropdow de perfil
     *
     * @param page manejador de pagina
     */
    void openProfileDropdown(Page page);

    /**
     * Click en el boton de cerrar sesion
     *
     * @param page manejador de pagina
     */
    void clickLogoutButton(Page page);

    /**
     * Hace click en cerrar sesion
     *
     * @param page manejador de pagina
     */
    void handleNpsSurvey(Page page);

    /**
     * Verifica si se cerro la sesion
     *
     * @param page manejador de pagina
     * @return {@link boolean}
     */
    boolean verifyLogoutSuccess(Page page);
}
