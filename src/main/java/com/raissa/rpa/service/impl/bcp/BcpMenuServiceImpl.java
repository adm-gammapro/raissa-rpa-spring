package com.raissa.rpa.service.impl.bcp;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.bcp.BcpMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BcpMenuServiceImpl implements BcpMenuService {
    public boolean verifyLoginSuccess(Page page) {
        log.info("Verificando si el login fue exitoso...");

        try {
            if (checkForLoginErrors(page)) {
                return false;
            }

            return waitAndVerifyLoginSuccess(page);

        } catch (InterruptedException e) {
            handleInterruptedException(e);
            return false;
        } catch (Exception e) {
            log.error("Error inesperado verificando login: {}", e.getMessage());
            return false;
        }
    }

    public void handleMobileModal(Page page) {
        try {
            Locator modal = MetodsGeneric.waitForVisible(page, "bcp-mobile-modal .bcp-modal-host-4-25-0.show", 5_000);

            log.info("Modal móvil detectado, intentando cerrar...");
            Locator closeIcon = MetodsGeneric.waitForVisible(page, "bcp-mobile-modal bcp-icon[name='close-r']", 2_000);
            Locator closeButton = closeIcon.locator("xpath=./ancestor::button");

            closeButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(3_000));

            MetodsGeneric.clickWithFallback(page, closeButton, 3_000);

            modal.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN)
                    .setTimeout(3_000));

            log.info("Modal móvil cerrado exitosamente");
        } catch (TimeoutError e) {
            log.warn("Timeout esperando a que el modal se cierre: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error manejando el modal móvil: {}", e.getMessage());
        }
    }

    public boolean isOnAccountsPage(Page page) {
        try {
            String[] accountsIndicators = {
                    "[class*='account']",
                    "[data-role*='account']",
                    "#accounts",
                    ".account-balance",
                    "bcp-account",
                    "table:has-text(\"Cuenta\")"
            };

            for (String selector : accountsIndicators) {
                if (buscaElemento(page, selector)) {
                    log.debug("Ya en página de cuentas - indicador: {}", selector);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Error verificando página de cuentas: {}", e.getMessage());
            return false;
        }
    }

    public void waitForAccountsToLoad(Page page) {
        try {
            log.debug("Esperando a que carguen los datos de cuentas...");

            MetodsGeneric.waitForVisible(page, "bcp-title-9nbaaa >> h1:has-text(\"Cuentas\")", 5_000);
        } catch (TimeoutError e) {
            log.debug("Botón 'Cuentas' por texto no visible: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error esperando carga de cuentas: {}", e.getMessage());
        }
    }

    public void waitForResumenAccountsToLoad(Page page) {
        try {
            log.debug("Esperando a que carguen los datos de resumen de cuentas...");

            page.waitForSelector(
                    "bcp-title-9nbaaa h1:has-text(\"Resumen de cuentas\")",
                    new Page.WaitForSelectorOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(3_000)
            );

            MetodsGeneric.randomWaitPage(page, 500, 800);
        } catch (TimeoutError e) {
            log.warn("Timeout, esperando a que carguen los datos de resumen de cuentas: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error esperando carga de resumen de cuentas: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> extractAccountsData(Page page) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        try {
            log.debug("Extrayendo datos reales de cuentas...");

            Locator dataTable = MetodsGeneric.waitForVisible(page, "bcp-data-table-9nbaaa", 15_000);

            Locator accountRows = dataTable.locator("xpath=.//bcp-table-row-9nbaaa[@index and string-length(@index) > 0]");
            long rowCount = accountRows.count();
            log.debug("Encontradas {} filas de cuentas", rowCount);

            for (int i = 0; i < rowCount; i++) {
                Locator row = accountRows.nth(i);
                Map<String, Object> accountData = extractAccountFromRow(row);
                if (!accountData.isEmpty()) {
                    accounts.add(accountData);
                    log.debug("Cuenta extraída: {}", accountData.get(Constantes.KEY_NUMERO_CUENTA));
                }
            }
            return accounts;
        } catch (Exception e) {
            log.error("Error extrayendo datos de cuentas: {}", e.getMessage());
            return accounts;
        }
    }

    public void navigateToResumen(Page page) {
        try {
            Locator resumenSel = MetodsGeneric.waitForVisible(
                    page,
                    "xpath=//div[contains(@class,'ms-child')]//p[normalize-space()='Resumen de Cuentas']",
                    2_000);
            if (!MetodsGeneric.clickWithFallback(page, resumenSel, 2_000)) {
                Locator cuentasSel = MetodsGeneric.waitForVisible(
                        page,
                        "xpath=//bcp-menu-sidebar//p[normalize-space()='Cuentas']",
                        2_000);
                if (MetodsGeneric.clickWithFallback(page, cuentasSel, 2_000)) {
                    MetodsGeneric.clickWithFallback(page, resumenSel, 2_000);
                }
            }

            log.warn("No se encontró la opción 'Resumen de Cuentas'");
        } catch (TimeoutError e) {
            log.warn("Timeout, esperando a que carguen los datos del resumen: {}", e.getMessage());
            throw new BcpException("Se supero tiempo de espera al navegar a resumen",
                    "BCP_NAVIGATION_ERROR",
                    "No se pudo acceder a la sección de resumen");
        } catch (Exception e) {
            log.warn("Error navegando a resumen: {}", e.getMessage());
            throw new BcpException("No se pudo navegar a resumen",
                    "BCP_NAVIGATION_ERROR",
                    "No se pudo acceder a la sección de resumen");
        }
    }

    public boolean selectCuenta(Page page, String numeroCuenta) {
        try {
            clickAccountsTab(page);

            String objetivo = numeroCuenta.replaceAll("\\D", "");

            Locator filas = page.locator("div.table-container div.cols-center-container bcp-table-row-9nbaaa[index]");

            long total = filas.count();
            for (int i = 0; i < total; i++) {
                Locator filaCuenta = filas.nth(i);
                Locator celdaCuenta = filaCuenta.locator("bcp-table-col-9nbaaa[index='0'] p.paragraph-sm.bcp-font-demi.text");
                if (celdaCuenta.count() == 0) continue;

                String cuentaTabla = celdaCuenta.first().innerText().replaceAll("\\D", "");
                if (!objetivo.equals(cuentaTabla)) continue;

                log.debug("Cuenta {} encontrada, preparando click en detalle", numeroCuenta);

                filaCuenta.hover(new Locator.HoverOptions().setTimeout(2_000));
                page.waitForTimeout(200);

                Locator iconoDetalle = filaCuenta.locator(".options-container bcp-icon-9nbaaa[name='eye-b']");
                if (iconoDetalle.count() == 0) {
                    log.warn("No se encontró el ícono de detalle en la fila de la cuenta {}", numeroCuenta);
                    return false;
                }

                MetodsGeneric.clickWithFallback(page, iconoDetalle, 5_000);
                esperarPantallaDetalle(page);
                return true;
            }

            log.warn("No se encontró cuenta para extraccion de movimientos {}", numeroCuenta);
            return false;
        } catch (TimeoutError te) {
            log.warn("Se supero tiempo de espera seleccionando cuenta {}: {}", numeroCuenta, te.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta para estraccion de movimientos {}: {}", numeroCuenta, e.getMessage());
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        }
    }

    public void setDateRange(Page page, String fechaInicio, String fechaFin) {
        log.info("Iniciando carga de rango de fechas");
        try {
            Locator fechaInicioInput = MetodsGeneric.waitForVisible(page, "bcp-input-bpbaaa input[name='inputDateFrom']", 5_000);

            Locator fechaFinInput = page.locator("bcp-input-bpbaaa input[name='inputDateTo']").first();

            fechaInicioInput.evaluate("(el, value) => { el.value = value; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); }", fechaInicio);
            fechaFinInput.evaluate("(el, value) => { el.value = value; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); }", fechaFin);

            MetodsGeneric.randomWaitPage(page, 500, 800);
        } catch (TimeoutError te) {
            log.warn("Timeout configurando rango de fechas {} - {}: {}", fechaInicio, fechaFin, te.getMessage());
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "Timeout configurando rango de fechas");
        } catch (Exception e) {
            log.warn("Error configurando rango de fechas {} - {}: {}", fechaInicio, fechaFin, e.getMessage());
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "Error configurando rango de fechas");
        }
    }

    public void applyFilters(Page page) {
        try {
            Locator buscarBtn = MetodsGeneric.waitForVisible(page, "bcp-button-bpbaaa[id-auto='clean-fiters-account-detail'] >> button", 10_000);

            MetodsGeneric.clickWithFallback(page, buscarBtn, 5_000);
            log.info("Búsqueda de movimientos ejecutada exitosamente");
        } catch (Exception e) {
            log.warn("Error aplicando filtros de búsqueda: {}", e.getMessage());
            throw new BcpException("No se pudo ejecutar la búsqueda");
        }
    }

    public void waitForMovimientosToLoad(Page page) {
        String anyResultSelector = String.join(", ",
                "bcp-data-table-bpbaaa.bcp-data-table-host",
                "bcp-table-row-bpbaaa[index]",
                "bcp-character-bpbaaa:has-text(\"FECHA\")",
                "bcp-character-bpbaaa:has-text(\"DESCRIPCIÓN\")",
                "bcp-character-bpbaaa:has-text(\"MONTO\")",
                "*:has-text(\"No se encontraron resultados\")",
                "*:has-text(\"sin resultados\")"
        );
        page.waitForSelector(anyResultSelector,
                new Page.WaitForSelectorOptions().setTimeout(30_000));
        MetodsGeneric.randomWaitPage(page, 1000, 2000);
    }

    public List<Map<String, Object>> extractMovimientosData(Page page) {
        List<Map<String, Object>> movimientos = new ArrayList<>();
        try {
            if (!hasMovimientosResults(page)) {
                log.info("No se encontraron movimientos");
                return movimientos;
            }

            page.waitForSelector("bcp-data-table-bpbaaa",
                    new Page.WaitForSelectorOptions().setTimeout(5_000));

            int totalPages = getTotalPages(page);

            extractPageData(page, movimientos);

            if (totalPages > 1) {
                for (int currentPage = 2; currentPage <= totalPages; currentPage++) {
                    navigateToPage(page, currentPage);
                    page.waitForSelector("bcp-data-table-bpbaaa",
                            new Page.WaitForSelectorOptions().setTimeout(5_000));
                    extractPageData(page, movimientos);
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo datos de movimientos: {}", e.getMessage());
        }
        return movimientos;
    }

    public boolean selectCuentaHistorico(Page page, String numeroCuenta) {
        try {
            // Asegura que la pestaña Cuentas esté abierta
            if (!clickAccountsTab(page)) {
                log.warn("No se pudo abrir la pestaña de cuentas");
                return false;
            }

            String objetivo = numeroCuenta.replaceAll("\\D", "");

            Locator filas = page.locator("div.table-container div.cols-center-container bcp-table-row-9nbaaa[index]");
            filas.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(8_000));

            long total = filas.count();
            for (int i = 0; i < total; i++) {
                Locator fila = filas.nth(i);
                Locator celdaCuenta = fila.locator("bcp-table-col-9nbaaa[index='0'] p.paragraph-sm.bcp-font-demi.text");
                if (celdaCuenta.count() == 0) continue;

                String cuentaTabla = celdaCuenta.first().innerText().replaceAll("\\D", "");
                if (!objetivo.equals(cuentaTabla)) continue;

                log.debug("Cuenta {} encontrada, preparando click en histórico", numeroCuenta);

                // Hover para mostrar iconos
                fila.hover(new Locator.HoverOptions().setTimeout(2_000));
                page.waitForTimeout(200);

                Locator iconoHistorico = fila.locator(".options-container bcp-icon-9nbaaa[name='clock-b']");
                if (iconoHistorico.count() == 0) {
                    log.warn("No se encontró el ícono de histórico en la fila de la cuenta {}", numeroCuenta);
                    return false;
                }

                MetodsGeneric.clickWithFallback(page, iconoHistorico, 5_000);
                esperarPantallaHistorico(page);
                return true;
            }

            log.warn("No se encontró la cuenta {}", numeroCuenta);
            return false;
        } catch (TimeoutError te) {
            log.warn("Timeout seleccionando cuenta {}: {}", numeroCuenta, te.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        }
    }

    public void applyFiltersHistorico(Page page) {
        try {
            Locator buscarBtn = page.locator("bcp-button-bpbaaa[id-auto='search-movements-button'] >> button");

            buscarBtn.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10_000));

            if (!buscarBtn.isEnabled()) {
                log.warn("El botón de búsqueda está deshabilitado, verificando validaciones...");
                page.locator("body").click();
                page.waitForTimeout(1_000);
                if (!buscarBtn.isEnabled()) {
                    throw new BcpException("Botón de búsqueda permanece deshabilitado después de ingresar fechas");
                }
            }

            buscarBtn.click(new Locator.ClickOptions().setTimeout(5_000));
            log.info("Búsqueda de movimientos historicos ejecutada exitosamente");

        } catch (Exception e) {
            log.warn("Error aplicando filtros de búsqueda hisotrica: {}", e.getMessage());
            throw new BcpException("No se pudo ejecutar la búsqueda");
        }
    }

    public void openProfileDropdown(Page page) {
        try {
            log.debug("Buscando dropdown de perfil...");

            String[] dropdownSelectors = {
                    "bcp-avatar[accessible-aria-label*='Avatar']",
                    "bcp-avatar.bcp-avatar-host-4-27-0",
                    "bcp-avatar[class*='bcp-avatar-host']",
                    "bcp-avatar[type='square']",
                    "bcp-avatar[bg-color='primary-400']",
                    ".avatar-container",
                    ".avatar-container-square",
                    "[aria-label*='Avatar']",
                    "bcp-avatar"
            };

            for (String sel : dropdownSelectors) {
                if (abreProfileDropdown(page, sel)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo abrir el dropdown de perfil: {}", e.getMessage());
        }
    }

    public void clickLogoutButton(Page page) {
        try {
            log.debug("Buscando botón de logout en footer-container...");

            String[] logoutSelectors = {
                    "div.footer-container bcp-button a.bcp-ffw-btn",
                    "div.footer-container bcp-icon[name='sign-out-r']",
                    "xpath=//div[@class='footer-container']//span[contains(@class,'character-container') and contains(normalize-space(),'Cerrar sesión')]"
            };

            for (String sel : logoutSelectors) {
                if (validaClicLogoutButton(page, sel)) {
                    return;
                }
            }

            throw new BcpException("No se pudo hacer logout", "BCP_LOGOUT_CLICK_ERROR",
                    "No se pudo encontrar el botón de cerrar sesión");

        } catch (Exception e) {
            log.error("Error haciendo click en logout: {}", e.getMessage());
            throw e;
        }
    }

    public void handleNpsSurvey(Page page) {
        try {
            log.debug("Verificando si aparece encuesta NPS...");

            String[] npsSelectors = {
                    "widget-nps",
                    "lib-nps",
                    ".nps",
                    "[class*='nps__']",
                    "bcp-paragraph:has-text(\"Según tu experiencia\")",
                    "bcp-paragraph:has-text(\"recomiendes\")"
            };

            for (String sel : npsSelectors) {
                if (buscaEncuesta(page, sel)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Error manejando encuesta NPS: {}", e.getMessage());
        }
    }

    public boolean verifyLogoutSuccess(Page page) {
        try {
            String[] logoutIndicators = {
                    "input[name='ciam-input-card']",
                    "button:has-text(\"Continuar\")",
                    "[class*='login']",
                    "xpath=//button[.//span[normalize-space()='Continuar']]"
            };

            if (checkAnyLogoutIndicator(page, logoutIndicators)) {
                return true;
            }

            String currentUrl = page.url();
            if (currentUrl != null &&
                    (currentUrl.contains("login") ||
                            currentUrl.contains("tarjeta-sesion") ||
                            currentUrl.contains("loginunico") ||
                            currentUrl.contains("/auth") ||
                            currentUrl.contains("/ciam"))) {
                log.debug("Logout verificado por cambio de URL: {}", currentUrl);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Error verificando logout: {}", e.getMessage());
            return false;
        }
    }

    public void manejarModalSesionExpirada(Page page) {
        try {
            Locator modal = MetodsGeneric.waitForVisible(page, "bcp-modal[is-open]", 5_000);
            Locator titulo = modal.locator("h3.title-sm");
            if (titulo.count() == 0 || !titulo.first().innerText().contains("expirado")) {
                log.debug("Modal visible pero no es de sesión expirada");
                return;
            }

            log.info("Modal 'Tu sesión ha expirado' detectado, haciendo click en 'Iniciar sesión'");

            Locator btnIniciarSesion = MetodsGeneric.waitForVisible(
                    page,
                    "bcp-button-ntlc-commons-widgets[id-auto='modal-manager-secondary'] button",
                    5_000
            );

            MetodsGeneric.clickWithFallback(page, btnIniciarSesion, 5_000);

            // Esperar redirección al login de viabcp
            page.waitForURL("**/tarjeta-sesion**", new Page.WaitForURLOptions().setTimeout(15_000));
            log.info("Redirigido al login: {}", page.url());
        } catch (TimeoutError te) {
            log.warn("Timeout esperando redirección al login: {}", te.getMessage());
            throw new BcpException("No se pudo navegar al login", "BCP_LOGIN_REDIRECT_ERROR", te.getMessage());
        } catch (Exception e) {
            log.warn("Error manejando modal de sesión expirada: {}", e.getMessage());
            throw new BcpException("Error en modal de sesión expirada", "BCP_MODAL_ERROR", e.getMessage());
        }
    }

    public boolean clickAccountsTab(Page page) {
        log.debug("Buscando tab 'Cuentas'...");
        try {
            Locator tab = MetodsGeneric.waitForVisible(
                    page,
                    "xpath=//bcp-tab-header-9nbaaa//button[.//span[normalize-space()='Cuentas']]",
                    5_000);


            MetodsGeneric.clickWithFallback(page, tab, 5_000);

            MetodsGeneric.randomWaitPage(page, 200, 500);

            log.debug("Tab 'Cuentas' clickeado con XPath específico");
            return true;
        } catch (TimeoutError e) {
            log.error("Se supero tiempo de espera haciendo click en tab Cuentas: {}", e.getMessage());
            throw new BcpException("No se pudo acceder a la sección de cuentas",
                    "BCP_ACCOUNTS_TAB_ERROR",
                    "No se pudo encontrar el tab de Cuentas");
        } catch (Exception e) {
            log.error("Error haciendo click en tab Cuentas: {}", e.getMessage());
            throw new BcpException("No se pudo acceder a la sección de cuentas",
                    "BCP_ACCOUNTS_TAB_ERROR",
                    "No se pudo encontrar el tab de Cuentas");
        }
    }

    /**
     * Espera a que cargue la página de busqueda historica
     *
     * @param page datos de pagina a ubicar
     */
    private void esperarPantallaHistorico(Page page) {
        try {
            page.waitForURL("**movimientos**", new Page.WaitForURLOptions().setTimeout(6_000));
            return;
        } catch (TimeoutError ignored) {
            log.error("Se supero tiempo de espera para pantalla de movimientos");
        }

        try {
            page.waitForURL("**historico**", new Page.WaitForURLOptions().setTimeout(6_000));
            return;
        } catch (TimeoutError ignored) {
            log.error("Se supero tiempo de espera para pantalla de historico");
        }

        // Luego por selectores clave
        String[] selectores = {
                "ibk-historical-date ibk-datepicker-range-v2",
                "[data-test='btnBuscarMovimientos']",
                "[data-test='btnEnter']",
                "xpath=//*[contains(@class,'historical-movements__filters')]"
        };
        for (String sel : selectores) {
            try {
                page.waitForSelector(sel,
                        new Page.WaitForSelectorOptions()
                                .setState(WaitForSelectorState.VISIBLE)
                                .setTimeout(6_000));
                return;
            } catch (TimeoutError ignored) {
                log.error("Se supero tiempo de espera para encontrar selector");
            }
        }
    }

    /**
     * Espera a que cargue la pagina de detalle
     *
     * @param page datos de pagina a ubicar
     */
    private void esperarPantallaDetalle(Page page) {
        try {
            page.waitForURL("**detalle**", new Page.WaitForURLOptions().setTimeout(8_000));
            return;
        } catch (TimeoutError ignored) {
            log.error("Se supero tiempo de espera para pagina de detalle");
        }

        String[] selectores = {
                "ibk-account-detail",
                "ibk-account-detail-info",
                "bcp-input-bpbaaa[name='inputDateFrom']",
                "bcp-button-bpbaaa[id-auto='clean-fiters-account-detail']",
                "xpath=//*[contains(text(), 'Detalle') and contains(text(), 'cuenta')]"
        };

        for (String sel : selectores) {
            try {
                page.waitForSelector(sel, new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(6_000));
                return;
            } catch (TimeoutError ignored) {
                log.error("Se supero tiempo de espera para ubicar la pantalla de detalle de cuenta");
            }
        }
    }

    /**
     * Extrae saldos y cuentas fila a fila
     *
     * @param row fila
     * @return {@link Map} datos de una cuenta por fila
     */
    private Map<String, Object> extractAccountFromRow(Locator row) {
        Map<String, Object> account = new HashMap<>();

        try {
            row.locator("p").first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(300));

            // 1. Número de cuenta
            Locator numeroCuentaElement = row.locator("xpath=.//bcp-paragraph-9nbaaa//p[contains(@class, 'paragraph-sm')]").first();
            String numeroCuenta = numeroCuentaElement.innerText().trim();
            numeroCuenta = MetodsGeneric.cleanAccountNumber(numeroCuenta);
            account.put(Constantes.KEY_NUMERO_CUENTA, numeroCuenta);

            // 2. Moneda
            Locator monedaElement = row.locator("xpath=.//bcp-table-col-9nbaaa[@index='3']//p").first();
            String moneda = monedaElement.innerText().trim();
            account.put(Constantes.KEY_MONEDA, moneda.toLowerCase().contains("sol") ? "PEN" : "USD");

            // 3. Saldo disponible (texto o error)
            Locator colSaldoDisp = row.locator("xpath=.//bcp-table-col-9nbaaa[@index='4']//p").first();
            if (colSaldoDisp.count() > 0) {
                String saldoStr = colSaldoDisp.innerText().trim();
                account.put(Constantes.KEY_SALDO_DISP, MetodsGeneric.parseSaldo(saldoStr));
            }

            // 4. Saldo contable (texto o error)
            Locator colSaldoCont = row.locator("xpath=.//bcp-table-col-9nbaaa[@index='6']//p").first();
            if (colSaldoCont.count() > 0) {
                String saldoStr = colSaldoCont.innerText().trim();
                account.put(Constantes.KEY_SALDO_CONT, MetodsGeneric.parseSaldo(saldoStr));
            }

            return account;
        } catch (Exception e) {
            log.error("Error extrayendo datos de fila: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Verifica si hubo errores en el formulario
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    private boolean checkForLoginErrors(Page page) {
        try {
            Locator errs = page.locator("bcp-alert, .alertConf, [class*='error'], [class*='alert']");
            int count = errs.count();
            for (int i = 0; i < count; i++) {
                String txt = errs.nth(i).innerText().toLowerCase();
                if (txt.contains("error") || txt.contains("incorrecto")
                        || txt.contains("inválido") || txt.contains("captcha")) {
                    log.error("Error detectado en página: {}", txt);
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            log.debug("No se pudieron verificar errores: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Espera y verifica si el login fue satisfactorio
     *
     * @param page manejador de página
     * @return {@link boolean}
     * @throws InterruptedException si hubo interrupcion
     */
    private boolean waitAndVerifyLoginSuccess(Page page) throws InterruptedException {
        waitForPageStabilization();

        boolean menuVisible = isSideMenuVisible(page);
        boolean dashboardLoaded = isDashboardLoaded(page);
        boolean userProfileVisible = isUserProfileVisible(page);

        log.debug("Indicadores login - Menu: {}, Dashboard: {}, Perfil: {}",
                menuVisible, dashboardLoaded, userProfileVisible);

        return menuVisible || dashboardLoaded || userProfileVisible;
    }

    /**
     * Espera para que la página se estabilice después del login
     *
     * @throws InterruptedException si hubo interrupcion
     */
    private void waitForPageStabilization() throws InterruptedException {
        log.debug("Esperando estabilización de la página post-login...");
        MetodsGeneric.randomWait(4500, 6500);
    }

    /**
     * Verifica si el menú lateral está visible como indicador de login exitoso
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    private boolean isSideMenuVisible(Page page) {
        try {
            Locator sideMenu = MetodsGeneric.waitForVisible(page, "bcp-menu-sidebar", 5_000);

            boolean visible = sideMenu.isVisible();
            log.info("Menú lateral encontrado y visible: {}", visible);

            Locator cuentas = page.locator("//bcp-menu-sidebar//p[normalize-space()='Cuentas']");
            if (cuentas.count() > 0 && cuentas.first().isVisible()) {
                log.info("Opción 'Cuentas' encontrada dentro del menú.");
            }
            return visible;
        } catch (Exception e) {
            log.warn("No se pudo encontrar/verificar el menú lateral: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si cargó el dashboard
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    private boolean isDashboardLoaded(Page page) {
        try {
            String[] selectors = {
                    "h1.title-lg",
                    ".card-balance__box",
                    "bcp-chart-line-9nbaaa",
                    "app-dashboard",
                    ".dashboard__summary",
                    "[class*='dashboard__']"
            };
            for (String sel : selectors) {
                if (buscaElemento(page, sel)) {
                    return true;
                }
            }
            log.error("Ningún selector de dashboard fue encontrado");
            return false;

        } catch (Exception e) {
            log.error("Error verificando dashboard: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si se muestra perfil de usuario
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    private boolean isUserProfileVisible(Page page) {
        try {
            String[] selectors = {
                    "bcp-avatar[accessible-aria-label*='Avatar']",
                    ".avatar-container",
                    ".bcp-character",
                    ".character-lg",
                    "[aria-label*='Avatar']",
                    "[class*='avatar']",
                    "bcp-avatar"
            };
            for (String sel : selectors) {
                if (buscaElemento(page, sel)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Error verificando perfil de usuario: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Busca si cargo el perfil
     *
     * @param page     Manejador de página
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean buscaElemento(Page page, String selector) {
        try {
            Locator loc = page.locator(selector);
            return loc.count() != 0;
        } catch (TimeoutError e) {
            log.debug("Selector {} no encontrado dentro del tiempo de espera", selector);
            return false;
        } catch (Exception e) {
            log.debug("Error con selector {}: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Abre dropdown de perfil
     *
     * @param page     manejador de página
     * @param selector elemento html
     */
    private boolean abreProfileDropdown(Page page, String selector) {
        try {
            Locator dropdown = MetodsGeneric.waitForVisible(page, selector, 3_000);
            MetodsGeneric.clickWithFallback(page, dropdown, 3_000);
            log.debug("Dropdown de perfil abierto con selector: {}", selector);
            return true;
        } catch (Exception e) {
            log.debug("Selector {} no funcionó al intentar abrir perfil: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Validar el elemento del logout para realizar clic
     *
     * @param page     manejador de página
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean validaClicLogoutButton(Page page, String selector) {
        try {
            Locator element = MetodsGeneric.waitForVisible(page, selector, 5_000);

            MetodsGeneric.clickWithFallback(page, element, 5_000);

            log.debug("Botón de logout clickeado con selector: {}", selector);

            MetodsGeneric.randomWaitPage(page, 1_000, 2_000);
            return true;
        } catch (TimeoutError te) {
            log.debug("Selector {} no visible/clicable a tiempo: {}", selector, te.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("Selector {} no funcionó: {}", selector, e.getMessage());
            return false;
        }
    }

    private boolean buscaEncuesta(Page page, String selector) {
        try {
            MetodsGeneric.waitForVisible(page, selector, 5_000);
            log.debug("Encuesta NPS detectada, cerrando...");
            closeNpsSurvey(page);
            return true;
        } catch (TimeoutError te) {
            log.debug("No se encontró encuesta con selector {}: {}", selector, te.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("Error verificando encuesta con selector {}: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Cierra encuesta
     *
     * @param page manejador de pagina
     */
    private void closeNpsSurvey(Page page) {
        String[] closeSelectors = {
                ".nps__close button",
                "bcp-button[shape='icon']",
                "bcp-icon[name='close-r']",
                "[aria-label*='close']",
                "[class*='close']"
        };

        for (String sel : closeSelectors) {
            if (tryCloseButtonWithSelector(page, sel)) {
                return;
            }
        }

        log.debug("No se pudo cerrar encuesta, esperando...");
        MetodsGeneric.randomWaitPage(page, 2_000, 3_000);
    }

    /**
     * Hace clic en el boton de cerrar de la encuesta
     *
     * @param page     manejador de pagina
     * @param selector selector de elemento
     * @return {@link boolean}
     */
    private boolean tryCloseButtonWithSelector(Page page, String selector) {
        try {
            Locator closeButton = MetodsGeneric.waitForVisible(page, selector, 3_000);
            if (closeButton.count() > 0 && closeButton.isVisible() && closeButton.isEnabled()) {
                MetodsGeneric.clickWithFallback(page, closeButton, 3_000);
                log.debug("Encuesta NPS cerrada con selector: {}", selector);
                MetodsGeneric.randomWaitPage(page, 1_500, 2_000);
                return true;
            }
        } catch (Exception e) {
            log.debug("Selector {} no funcionó para cerrar encuesta: {}", selector, e.getMessage());
        }
        return false;
    }

    /**
     * Chequea si hay algun selector que indique que se cerror la sesion exitosamente
     *
     * @param page      maenjador de pagina
     * @param selectors selector a buscar
     * @return {@link boolean}
     */
    private boolean checkAnyLogoutIndicator(Page page, String[] selectors) {
        for (String sel : selectors) {
            if (checkLogoutWithSelector(page, sel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cuerpo de la comparacion de selector que verifica logout
     *
     * @param page     manejador de pagina
     * @param selector selector ubicar
     * @return {@link boolean}
     */
    private boolean checkLogoutWithSelector(Page page, String selector) {
        try {
            Locator elements = page.locator(selector);
            if (elements.count() > 0 && elements.first().isVisible()) {
                log.debug("Logout verificado con selector: {}", selector);
                return true;
            }
        } catch (Exception e) {
            log.debug("Selector {} no funcionó para verificar logout: {}", selector, e.getMessage());
        }
        return false;
    }

    private boolean hasMovimientosResults(Page page) {
        try {
            Locator filasResultados = page.locator("xpath=//bcp-table-row-bpbaaa[@index and not(contains(@class, 'header'))]");
            if (filasResultados.count() > 0) {
                log.info("Se encontraron {} movimientos", filasResultados.count());
                return true;
            }
            Locator mensajeSinResultados = page.locator("xpath=//*[contains(text(), 'No se encontraron resultados') or contains(text(), 'sin resultados')]");
            if (mensajeSinResultados.count() > 0) {
                log.info("No se encontraron movimientos para el rango de fechas especificado");
                return false;
            }
            return false;
        } catch (Exception e) {
            log.warn("Error verificando resultados de movimientos: {}", e.getMessage());
            return false;
        }
    }

    private int getTotalPages(Page page) {
        try {
            Locator pages = page.locator("bcp-pagination-bpbaaa li.page");
            long count = pages.count();
            return count > 0 ? (int) count : 1;
        } catch (Exception e) {
            log.debug("No se encontró paginación, asumiendo 1 página");
            return 1;
        }
    }

    private void navigateToPage(Page page, int targetPage) {
        Locator paginacion = MetodsGeneric.waitForVisible(page, "bcp-pagination-bpbaaa li.page >> text=\"" + targetPage + "\"", 4_000);
        MetodsGeneric.clickWithFallback(page, paginacion, 3_000);
        MetodsGeneric.randomWaitPage(page, 300, 600);
    }

    private void extractPageData(Page page, List<Map<String, Object>> movimientos) {
        try {
            Locator filas = page.locator("bcp-table-row-bpbaaa[index]");
            long total = filas.count();
            for (int i = 0; i < total; i++) {
                try {
                    Locator fila = filas.nth(i);
                    Locator columnas = fila.locator("bcp-table-col-bpbaaa");
                    long cols = columnas.count();
                    if (cols >= 6) {
                        Map<String, Object> mov = new HashMap<>();

                        String fecha = getColumnText(columnas.nth(0));
                        String fechaValor = getColumnText(columnas.nth(1));
                        String descripcion = getColumnText(columnas.nth(2));
                        String operacion = getColumnText(columnas.nth(3));
                        String montoText = getColumnText(columnas.nth(4));
                        double monto = MetodsGeneric.parseSaldo(montoText);
                        String saldoText = getColumnText(columnas.nth(4));
                        double saldo = MetodsGeneric.parseSaldo(saldoText);

                        mov.put("fecha", fecha);
                        mov.put("fecha_valor", fechaValor);
                        mov.put("descripcion", descripcion);
                        mov.put("operacion", operacion);
                        mov.put("monto", monto);
                        mov.put("tipo", montoText.contains("-") ? "DEBITO" : "CREDITO");
                        mov.put("saldo", saldo);
                        mov.put("referencia", "-");

                        movimientos.add(mov);
                    }
                } catch (Exception e) {
                    log.warn("Error procesando fila: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error extrayendo datos de página: {}", e.getMessage());
        }
    }

    /**
     * @param columna extrae el valor de la columna enviada como parametro
     * @return {@link String} valor de la columna
     */
    private String getColumnText(Locator columna) {
        try {
            Locator paragraphs = columna.locator("bcp-paragraph-bpbaaa");
            if (paragraphs.count() > 0) {
                return paragraphs.first().innerText().trim();
            }
            return columna.innerText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Maneja excepciones de interrupción de manera consistente
     *
     * @param e error de interrupcion
     */
    private void handleInterruptedException(InterruptedException e) throws BcpException {
        log.error("Interrupción durante la navegación: {}", e.getMessage());
        Thread.currentThread().interrupt();
        throw new BcpException("Interrupción durante la navegación",
                "BCP_NAVIGATION_INTERRUPTED",
                "El proceso fue interrumpido durante la navegación");
    }
}
