package com.raissa.rpa.service.ibk;

import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;

public interface IbkMenuService {
    /**
     * Verifica si el logueo fue exitoso
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    boolean verifyLoginSuccess(WebDriver driver);

    boolean closeCampaignPopupIfPresent(WebDriver driver);

    /**
     * Hace clic en la opcion consultas saldos
     * @param driver manejador de página
     * @return {@link boolean}
     */
    boolean clickConsultas(WebDriver driver);

    void waitForAccountsToLoad(WebDriver driver);

    List<Map<String, Object>> extractAccountsData(WebDriver driver);

    boolean clickMovimientos(WebDriver driver);

    void waitForMovementsToLoad(WebDriver driver);

    boolean selectCuenta(WebDriver driver, String numeroCuenta);

    boolean setDateRange(WebDriver driver, String fechaInicio, String fechaFin);

    void applyFilters(WebDriver driver);

    boolean waitForMovimientosToLoad(WebDriver driver);

    List<Map<String, Object>> extractMovimientosData(WebDriver driver);

    void openProfileDropdown(WebDriver driver);

    void clickLogoutButton(WebDriver driver);

    void handleNpsSurvey(WebDriver driver);

    boolean verifyLogoutSuccess(WebDriver driver);
}
