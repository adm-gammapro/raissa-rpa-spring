package com.raissa.rpa.service.impl.bcp;

import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.bcp.BcpMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class BcpMenuServiceImpl implements BcpMenuService {
    public boolean verifyLoginSuccess(WebDriver driver) {
        log.info("Verificando si el login fue exitoso...");

        try {
            if (checkForLoginErrors(driver)) {
                return false;
            }

            return waitAndVerifyLoginSuccess(driver);

        } catch (InterruptedException e) {
            handleInterruptedException(e);
            return false;
        } catch (Exception e) {
            log.error("Error inesperado verificando login: {}", e.getMessage());
            return false;
        }
    }

    public void handleMobileModal(WebDriver driver) {
        try {
            MetodsGeneric.randomWait(5000, 7000);

            List<WebElement> modals = driver.findElements(By.cssSelector("bcp-mobile-modal .bcp-modal-host-4-25-0.show"));

            if (!modals.isEmpty() && modals.get(0).isDisplayed()) {
                try {
                    WebElement closeIcon = driver.findElement(By.cssSelector("bcp-mobile-modal bcp-icon[name='close-r']"));
                    WebElement closeButton = closeIcon.findElement(By.xpath("./ancestor::button"));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeButton);
                    log.info("Modal cerrado a través del ícono");
                } catch (NoSuchElementException e2) {
                    log.error("No se pudo encontrar el botón de cerrar");
                }

            } else {
                log.info("No se detectó modal móvil abierto");
            }
        } catch (NoSuchElementException e) {
            log.info("No se encontró el modal móvil: {}", e.getMessage());
        } catch (TimeoutException e) {
            log.warn("Timeout esperando a que el modal se cierre: {}", e.getMessage());
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (Exception e) {
            log.warn("Error manejando el modal móvil: {}", e.getMessage());
        }
    }

    public void clickAccountsTab(WebDriver driver) {
        try {
            log.debug("Buscando tab 'Cuentas'...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            if(seleccionarTabCuentasXpath(wait)) {
                return;
            }

            throw new NoSuchElementException("No se pudo encontrar el tab de Cuentas");

        } catch (Exception e) {
            log.error("Error haciendo click en tab Cuentas: {}", e.getMessage());
            throw new BcpException("No se pudo acceder a la sección de cuentas",
                    "BCP_ACCOUNTS_TAB_ERROR",
                    "No se pudo encontrar el tab de Cuentas");
        }
    }

    public boolean isOnAccountsPage(WebDriver driver) {
        try {
            String[] accountsIndicators = {
                    "[class*='account']",
                    "[data-role*='account']",
                    "#accounts",
                    ".account-balance",
                    "bcp-account", // Componente de cuenta
                    "table:contains('Cuenta')" // Tabla con información de cuentas
            };

            for (String selector : accountsIndicators) {
                return verificarPaginaCuentas(driver, selector);
            }

            return false;

        } catch (Exception e) {
            log.debug("Error verificando página de cuentas: {}", e.getMessage());
            return false;
        }
    }

    public void waitForAccountsToLoad(WebDriver driver) {
        try {
            log.debug("Esperando a que carguen los datos de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//bcp-title-9nbaaa//h1[normalize-space()='Cuentas']")
            ));

            Thread.sleep(500);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (Exception e) {
            log.warn("Error esperando carga de cuentas: {}", e.getMessage());
        }
    }

    public void waitForResumenAccountsToLoad(WebDriver driver) {
        try {
            log.debug("Esperando a que carguen los datos de resumen de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//bcp-title-9nbaaa//h1[normalize-space()='Resumen de cuentas']")
            ));
            Thread.sleep(500);

            WebElement cuentasOption = driver.findElement(By.xpath("//bcp-menu-sidebar//p[normalize-space()='Cuentas']"));
            cuentasOption.click();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (Exception e) {
            log.warn("Error esperando carga de resumen de cuentas: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> extractAccountsData(WebDriver driver) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        try {
            log.debug("Extrayendo datos reales de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement dataTable = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("bcp-data-table-9nbaaa")
            ));

            List<WebElement> accountRows = dataTable.findElements(By.xpath(
                    ".//bcp-table-row-9nbaaa[@index and string-length(@index) > 0]"
            ));

            log.debug("Encontradas {} filas de cuentas", accountRows.size());

            for (WebElement row : accountRows) {
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

    public boolean navigateToResumen(WebDriver driver) {
        try {
            WebElement cuentasOption = driver.findElement(By.xpath("//bcp-menu-sidebar//p[normalize-space()='Cuentas']"));
            cuentasOption.click();

            Thread.sleep(200);

            WebElement resumenOption = driver.findElement(By.xpath("//div[@class='ms-child']//p[normalize-space()='Resumen de Cuentas']"));
            resumenOption.click();

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error navegando a resumen: {}", e.getMessage());
            return false;
        }
    }

    public boolean selectCuenta(WebDriver driver, String numeroCuenta) {
        try {

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            if(seleccionarTabCuentasXpath(wait)) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-table-row-9nbaaa[contains(@index, '" + numeroCuenta + "')]")
                ));

                WebElement botonMovimientos = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(".//bcp-icon-9nbaaa[@name='eye-b']")
                ));

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", botonMovimientos);
                Thread.sleep(500);

                botonMovimientos.click();

                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//*[contains(text(), 'Detalle') or contains(text(), 'cuenta')]")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-datepicker-range-bpbaaa[@id-auto='range-last-movements']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//input[@name='inputDateFrom']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//input[@name='inputDateTo']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-button-bpbaaa[@id-auto='clean-fiters-account-detail']")
                        )
                ));

                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        }
    }

    public boolean setDateRange(WebDriver driver, String fechaInicio, String fechaFin) {
        log.info("Iniciando carga de rango de fechas");
        try {
            MetodsGeneric.randomWait(2000, 3000);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//bcp-input-bpbaaa//input[@name='inputDateFrom']")
            ));

            WebElement fechaInicioInput = driver.findElement(
                    By.xpath("//bcp-input-bpbaaa//input[@name='inputDateFrom']")
            );

            WebElement fechaFinInput = driver.findElement(
                    By.xpath("//bcp-input-bpbaaa//input[@name='inputDateTo']")
            );

            // Establecer valores directamente con JavaScript
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", fechaInicioInput, fechaInicio);
            ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", fechaFinInput, fechaFin);

            // Disparar eventos para que se registren los cambios
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", fechaInicioInput);
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fechaInicioInput);
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", fechaFinInput);
            ((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", fechaFinInput);

            MetodsGeneric.randomWait(500, 800);

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error configurando rango de fechas {} - {}: {}", fechaInicio, fechaFin, e.getMessage());
            return false;
        }
    }

    public void applyFilters(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//bcp-button-bpbaaa[@id-auto='clean-fiters-account-detail']//button")
            ));

            WebElement buscarBtn = driver.findElement(
                    By.xpath("//bcp-button-bpbaaa[@id-auto='clean-fiters-account-detail']//button")
            );

            if (!buscarBtn.isEnabled()) {
                log.warn("El botón de búsqueda está deshabilitado, verificando validaciones...");

                driver.findElement(By.tagName("body")).click();
                Thread.sleep(1000);

                if (!buscarBtn.isEnabled()) {
                    throw new BcpException("Botón de búsqueda permanece deshabilitado después de ingresar fechas");
                }
            }

            buscarBtn.click();

            log.info("Búsqueda de movimientos ejecutada exitosamente");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error aplicando filtros de búsqueda: {}", e.getMessage());
            throw new BcpException("No se pudo ejecutar la búsqueda");
        }
    }

    public void waitForMovimientosToLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-data-table-bpbaaa[contains(@class, 'bcp-data-table-host')]")
                ),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-table-row-bpbaaa[@index]")
                ),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-character-bpbaaa[contains(., 'FECHA')]")
                ),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-character-bpbaaa[contains(., 'DESCRIPCIÓN')]")
                ),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-character-bpbaaa[contains(., 'MONTO')]")
                ),
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//*[contains(text(), 'No se encontraron resultados') or contains(text(), 'sin resultados')]")
                )
        ));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<Map<String, Object>> extractMovimientosData(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();

        try {
            if (!hasMovimientosResults(driver)) {
                log.info("No se encontraron movimientos");
                return movimientos;
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("bcp-data-table-bpbaaa")));

            int totalPages = getTotalPages(driver);

            extractPageData(driver, movimientos);

            if (totalPages > 1) {
                for (int currentPage = 2; currentPage <= totalPages; currentPage++) {
                    navigateToPage(driver, currentPage);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("bcp-data-table-bpbaaa")));

                    extractPageData(driver, movimientos);
                }
            }

        } catch (Exception e) {
            log.warn("Error extrayendo datos de movimientos: {}", e.getMessage());
        }

        return movimientos;
    }

    public boolean selectCuentaHistorico(WebDriver driver, String numeroCuenta) {
        try {

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            if(seleccionarTabCuentasXpath(wait)) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//bcp-table-row-9nbaaa[contains(@index, '" + numeroCuenta + "')]")
                ));

                WebElement botonMovimientos = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath(".//bcp-icon-9nbaaa[@name='clock-b']")
                ));

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", botonMovimientos);
                Thread.sleep(500);

                botonMovimientos.click();

                wait.until(ExpectedConditions.or(
                        ExpectedConditions.urlContains("movimientos"),
                        ExpectedConditions.urlContains("historico"),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//*[contains(text(), 'Movimientos') or contains(text(), 'Histórico')]")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-datepicker-range-bpbaaa[@id-auto='range-movements']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-input-bpbaaa[@name='inputDateFrom']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-input-bpbaaa[@name='inputDateTo']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-select-bpbaaa[@id-auto='historical-movement-type']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//bcp-button-bpbaaa[@id-auto='search-movements-button']")
                        ),
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//*[contains(@class, 'historical-movements__filters')]")
                        )
                ));

                return true;
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        }
    }

    public void applyFiltersHistorico(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//bcp-button-bpbaaa[@id-auto='search-movements-button']//button")
            ));

            WebElement buscarBtn = driver.findElement(
                    By.xpath("//bcp-button-bpbaaa[@id-auto='search-movements-button']//button")
            );

            if (!buscarBtn.isEnabled()) {
                log.warn("El botón de búsqueda está deshabilitado, verificando validaciones...");

                driver.findElement(By.tagName("body")).click();
                Thread.sleep(1000);

                if (!buscarBtn.isEnabled()) {
                    throw new BcpException("Botón de búsqueda permanece deshabilitado después de ingresar fechas");
                }
            }

            buscarBtn.click();

            log.info("Búsqueda de movimientos ejecutada exitosamente");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error aplicando filtros de búsqueda: {}", e.getMessage());
            throw new BcpException("No se pudo ejecutar la búsqueda");
        }
    }

    public void openProfileDropdown(WebDriver driver) {
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

            for (String selector : dropdownSelectors) {
                if(abreProfileDropdown(driver, selector)){
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo abrir el dropdown de perfil: {}", e.getMessage());
        }
    }

    public void clickLogoutButton(WebDriver driver) {
        try {
            log.debug("Buscando botón de logout en footer-container...");

            String[] logoutSelectors = {
                    "//div[@class='footer-container']//bcp-button[@mode='light']//*[normalize-space()='Cerrar sesión']",
                    "//div[@class='footer-container']//bcp-button[@class='bcp-button-host-4-27-0 hydrated']//*[normalize-space()='Cerrar sesión']",
                    "//div[@class='footer-container']//*[normalize-space()='Cerrar sesión']",
                    "//div[@class='footer-container']//bcp-icon[@name='sign-out-r']/following-sibling::text()[contains(., 'Cerrar sesión')]/..",
                    "div.footer-container > bcp-button[type='button']",
                    "div.footer-container bcp-button[mode='light']",
                    "//div[@class='footer-container']//bcp-button//a[contains(@class, 'bcp-ffw-btn')]//span[normalize-space()='Cerrar sesión']"
            };

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            for (String selector : logoutSelectors) {
                if(validaClicLogoutButton(driver, wait, selector)) {
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Error haciendo click en logout: {}", e.getMessage());
            throw new BcpException("No se pudo hacer logout", "BCP_LOGOUT_CLICK_ERROR",
                    "No se pudo encontrar el botón de cerrar sesión");
        }
    }

    public void handleNpsSurvey(WebDriver driver) {
        try {
            log.debug("Verificando si aparece encuesta NPS...");

            String[] npsSelectors = {
                    "widget-nps",
                    "lib-nps",
                    ".nps",
                    "[class*='nps__']",
                    "bcp-paragraph:contains('Según tu experiencia')",
                    "bcp-paragraph:contains('recomiendes')"
            };

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            for (String selector : npsSelectors) {
                if(buscaEncuesta(driver, wait, selector)){
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Error manejando encuesta NPS: {}", e.getMessage());
        }
    }

    public boolean verifyLogoutSuccess(WebDriver driver) {
        try {
            String[] logoutIndicators = {
                    "input[name='ciam-input-card']",
                    "//button//span[normalize-space()='Continuar']",
                    "[class*='login']",
                    "body:not(:has(.dashboard))"
            };

            boolean logoutDetected = checkAnyLogoutIndicator(driver, logoutIndicators);
            if (logoutDetected) {
                return true;
            }

            String currentUrl = driver.getCurrentUrl();
            if (currentUrl != null &&
                    (currentUrl.contains("login") ||
                            currentUrl.contains("tarjeta-sesion") ||
                            currentUrl.contains("loginunico"))) {
                log.debug("Logout verificado por cambio de URL: {}", currentUrl);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Error verificando logout: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Seleccionar tab con selectores xpath
     * @param wait tiempo de espera para busqueda
     * @return {@link boolean}
     */
    private boolean seleccionarTabCuentasXpath(WebDriverWait wait) {
        try {
            WebElement tabElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//bcp-tab-header-9nbaaa//button[.//span[normalize-space()='Cuentas']]"
            )));

            tabElement.click();
            log.debug("Tab 'Cuentas' clickeado con XPath específico");
            Thread.sleep(500);
            return true;

        } catch (InterruptedException e) {
            handleInterruptedException(e);
            return false;
        } catch (Exception e) {
            log.debug("XPath específico también falló: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ubicar elementos del tab de cuentas
     *
     * @param driver manejador de páginas
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean verificarPaginaCuentas(WebDriver driver,
                                           String selector) {
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector(selector));
            if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                log.debug("Ya en página de cuentas - indicador: {}", selector);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.debug("Indicador {} no visible: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Extrae saldos y cuentas fila a fila
     * @param row fila
     * @return {@link Map} datos de una cuenta por fila
     */
    private Map<String, Object> extractAccountFromRow(WebElement row) {
        Map<String, Object> account = new HashMap<>();

        try {
            // 1. Número de cuenta
            WebElement numeroCuentaElement = row.findElement(By.xpath(
                    ".//bcp-paragraph-9nbaaa[@size='sm' and @color='text' and @family='demi']/p[@class='paragraph-sm bcp-font-demi text']"
            ));
            String numeroCuenta = numeroCuentaElement.getText().trim();
            numeroCuenta = MetodsGeneric.cleanAccountNumber(numeroCuenta);
            account.put(Constantes.KEY_NUMERO_CUENTA, numeroCuenta);

            // 2. Moneda
            WebElement monedaElement = row.findElement(By.xpath(
                    ".//bcp-table-col-9nbaaa[@index='3']//bcp-paragraph-9nbaaa/p[@class='paragraph-sm bcp-font-regular onsurface-800']"
            ));
            String moneda = monedaElement.getText().trim();
            account.put(Constantes.KEY_MONEDA, moneda.equals("Soles") ? "PEN" : "USD");

            // 3. Saldo disponible
            WebElement saldoDisponibleElement = null;
            List<WebElement> elements = row.findElements(By.xpath(
                    ".//bcp-table-col-9nbaaa[@index='4']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi text']"
            ));
            if (!elements.isEmpty()) {
                saldoDisponibleElement = elements.get(0);
            } else {
                elements = row.findElements(By.xpath(
                        ".//bcp-table-col-9nbaaa[@index='4']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi error']"
                ));
                if (!elements.isEmpty()) {
                    saldoDisponibleElement = elements.get(0);
                }
            }
            if (saldoDisponibleElement == null) {
                log.warn("No se pudo encontrar el elemento de saldo disponible");
                return Collections.emptyMap();
            }

            String saldoDisponibleStr = saldoDisponibleElement.getText().trim();
            double saldoDisponible = MetodsGeneric.parseSaldo(saldoDisponibleStr);
            account.put(Constantes.KEY_SALDO_DISP, saldoDisponible);

            // 4. Saldo contable
            WebElement saldoContableElement = null;
            List<WebElement> elementsc = row.findElements(By.xpath(
                    ".//bcp-table-col-9nbaaa[@index='6']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi text']"
            ));
            if (!elementsc.isEmpty()) {
                saldoContableElement = elementsc.get(0);
            } else {
                elementsc = row.findElements(By.xpath(
                        ".//bcp-table-col-9nbaaa[@index='6']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi error']"
                ));
                if (!elementsc.isEmpty()) {
                    saldoContableElement = elementsc.get(0);
                }
            }
            if (saldoContableElement == null) {
                log.warn("No se pudo encontrar el elemento de saldo disponible");
                return Collections.emptyMap();
            }

            String saldoContableStr = saldoContableElement.getText().trim();
            double saldoContable = MetodsGeneric.parseSaldo(saldoContableStr);
            account.put(Constantes.KEY_SALDO_CONT, saldoContable);

            return account;

        } catch (Exception e) {
            log.error("Error extrayendo datos de fila: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Verifica si hubo errores en el formulario
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean checkForLoginErrors(WebDriver driver) {
        try {
            List<WebElement> errorElements = driver.findElements(
                    By.cssSelector("bcp-alert, .alertConf, [class*='error'], [class*='alert']")
            );

            for (WebElement errorElement : errorElements) {
                String errorText = errorElement.getText().toLowerCase();

                if (errorText.contains("error") || errorText.contains("incorrecto") ||
                        errorText.contains("inválido") || errorText.contains("captcha")) {

                    log.error("Error detectado en página: {}", errorText);
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
     * @param driver manejador de página
     * @return {@link boolean}
     * @throws InterruptedException si hubo interrupcion
     */
    private boolean waitAndVerifyLoginSuccess(WebDriver driver) throws InterruptedException {
        waitForPageStabilization();

        boolean menuVisible = isSideMenuVisible(driver);
        boolean dashboardLoaded = isDashboardLoaded(driver);
        boolean userProfileVisible = isUserProfileVisible(driver);

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
        Thread.sleep(3000);
    }

    /**
     * Verifica si el menú lateral está visible como indicador de login exitoso
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean isSideMenuVisible(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement sideMenu = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("bcp-menu-sidebar"))
            );

            boolean isMenuVisible = sideMenu.isDisplayed();
            log.info("Menú lateral encontrado y visible: {}", isMenuVisible);

            WebElement cuentas = driver.findElement(By.xpath("//bcp-menu-sidebar//p[normalize-space()='Cuentas']"));
            if (cuentas.isDisplayed()) {
                log.info("Opción 'Cuentas' encontrada dentro del menú.");
            }

            return isMenuVisible;

        } catch (Exception e) {
            log.warn("No se pudo encontrar/verificar el menú lateral: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si cargó el dashboard
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean isDashboardLoaded(WebDriver driver) {
        try {
            String[] dashboardSelectors = {
                    "h1.title-lg", // Título "Resumen de cuentas"
                    ".card-balance__box", // Tarjetas de saldo
                    "bcp-chart-line-9nbaaa", // Gráfico de líneas
                    "app-dashboard", // Componente Angular
                    ".dashboard__summary", // Sección resumen
                    "[class*='dashboard__']" // Cualquier elemento con clase que contenga "dashboard__"
            };

            for (String selector : dashboardSelectors) {
                return verificarLoadedDashboard(driver, selector);
            }

            log.error("Ningún selector de dashboard fue encontrado");
            return false;

        } catch (Exception e) {
            log.error("Error verificando dashboard: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el dashboard se cargó
     * @param driver manejador de página
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean verificarLoadedDashboard(WebDriver driver,
                                             String selector) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));

            if (element.isDisplayed()) {
                log.debug("Dashboard encontrado con selector: {}", selector);

                if (selector.equals("h1.title-lg")) {
                    String titleText = element.getText();
                    if (titleText.contains("Resumen de cuentas")) {
                        log.info("Título del dashboard confirmado: {}", titleText);
                        return true;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Selector {} no encontrado: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si se muestra perfil de usuario
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean isUserProfileVisible(WebDriver driver) {
        try {
            String[] profileSelectors = {
                    "bcp-avatar[accessible-aria-label*='Avatar']", // Selector específico del avatar
                    ".avatar-container", // Contenedor del avatar
                    ".bcp-character", // Componente de caracteres
                    ".character-lg", // Texto grande (iniciales del usuario)
                    "[aria-label*='Avatar']", // Cualquier elemento con "Avatar" en el label
                    "[class*='avatar']", // Clases que contengan "avatar"
                    "bcp-avatar" // El componente de avatar directamente
            };

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            for (String selector : profileSelectors) {
                return buscaElementoPerfil(wait, selector);
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
     * @param wait espera del manejador de página
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean buscaElementoPerfil(WebDriverWait wait,
                                        String selector){
        try {
            List<WebElement> elements = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector(selector)));
            if (!elements.isEmpty()) {
                log.debug("Perfil de usuario encontrado con selector: {}", selector);
                return true;
            }
            return false;
        } catch (TimeoutException e) {
            log.debug("Selector {} no encontrado dentro del tiempo de espera", selector);
            return false;
        } catch (Exception e) {
            log.debug("Error con selector {}: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Abre dropdown de perfil
     * @param driver manejador de página
     * @param selector elemento html
     */
    private boolean abreProfileDropdown(WebDriver driver,
                                        String selector) {
        try {
            WebElement dropdown = driver.findElement(By.cssSelector(selector));
            if (dropdown.isDisplayed()) {
                dropdown.click();
                log.debug("Dropdown de perfil abierto con selector: {}", selector);
                Thread.sleep(500);
                return true;
            }
            return false;
        } catch (InterruptedException e) {
            handleInterruptedException(e);
            log.error("Selector {} no funcionó al intentar abrir perfil: {}", selector, e.getMessage());

            return false;
        }
    }

    /**
     * Validar el elemento del logout para realizar clic
     * @param driver manejador de página
     * @param wait tiempo de espera al ubicar elemento
     * @param selector elemento html
     * @return {@link boolean}
     */
    private boolean validaClicLogoutButton(WebDriver driver,
                                           WebDriverWait wait,
                                           String selector) {
        try {
            WebElement element;

            if (selector.startsWith("//")) {
                element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(selector)));
            } else {
                element = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(selector)));
            }

            if (isValidLogoutButton(element)) {
                clickElementWithRetry(element, driver);
                log.debug("Botón de logout clickeado con selector: {}", selector);
                Thread.sleep(2000);
                return true;
            }

            return false;
        } catch (InterruptedException interr) {
            handleInterruptedException(interr);
            log.error("Se interrumpe clic: {}", interr.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Selector {} no funcionó: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Valida si el boton de logout es válido
     * @param element elemento en página
     * @return {@link boolean}
     */
    private boolean isValidLogoutButton(WebElement element) {
        try {
            if (!element.isDisplayed() || !element.isEnabled()) {
                return false;
            }

            String text = element.getText().toLowerCase();
            String tagName = element.getTagName().toLowerCase();

            boolean hasLogoutText = text.contains("cerrar") ||
                    text.contains("logout") ||
                    text.contains("salir") ||
                    text.contains("sign out");

            boolean isClickable = tagName.equals("a") ||
                    tagName.equals("button") ||
                    tagName.equals("input");

            return hasLogoutText && isClickable;

        } catch (Exception e) {
            log.debug("Error validando botón de logout: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Hace clic en el elemento de cerrar sesión
     * @param element elemento en la página
     * @param driver manejador de página
     */
    private void clickElementWithRetry(WebElement element, WebDriver driver) {
        try {
            element.click();
        } catch (Exception e) {
            log.debug("Click normal falló, intentando con JavaScript: {}", e.getMessage());

            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            } catch (Exception jsEx) {
                log.debug("JavaScript click también falló: {}", jsEx.getMessage());

                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].style.display='block'; arguments[0].style.visibility='visible';", element);
                    Thread.sleep(500);
                    element.click();
                } catch (InterruptedException interr) {
                    handleInterruptedException(interr);
                    log.warn("Se interrumpe click en logout: {}", interr.getMessage());
                } catch (Exception finalEx) {
                    throw new BcpException("No se pudo hacer click en el elemento");
                }
            }
        }
    }

    private boolean buscaEncuesta(WebDriver driver,
                                  WebDriverWait wait,
                                  String selector) {
        try {
            WebElement survey = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
            if (survey.isDisplayed()) {
                log.debug("Encuesta NPS detectada, cerrando...");
                closeNpsSurvey(driver);
                return true;
            }
            return false;
        } catch (TimeoutException e) {
            log.error("Tiempo de espera sobrepasado");
            return false;
        } catch (Exception e) {
            log.error("Error verificando encuesta con selector {}: {}", selector, e.getMessage());
            return false;
        }
    }

    /**
     * Cierra encuesta
     *
     * @param driver manejador de pagina
     */
    private void closeNpsSurvey(WebDriver driver) {
        try {
            String[] closeSelectors = {
                    ".nps__close button",
                    "bcp-button[shape='icon']",
                    "bcp-icon[name='close-r']",
                    "[aria-label*='close']",
                    "[class*='close']"
            };

            for (String selector : closeSelectors) {
                boolean closed = tryCloseButtonWithSelector(driver, selector);
                if (closed) {
                    return;
                }
            }

            log.debug("No se pudo cerrar encuesta, esperando...");
            Thread.sleep(3000); // Esperar a que posiblemente desaparezca sola

        } catch (InterruptedException e) {
            handleInterruptedException(e);
            log.warn("Error cerrando encuesta NPS: {}", e.getMessage());
        }
    }

    /**
     * Hace clic en el boton de cerrar de la encuesta
     *
     * @param driver manejador de pagina
     * @param selector selector de elemento
     * @return {@link boolean}
     */
    private boolean tryCloseButtonWithSelector(WebDriver driver, String selector) {
        try {
            WebElement closeButton = driver.findElement(By.cssSelector(selector));
            if (closeButton.isDisplayed() && closeButton.isEnabled()) {
                closeButton.click();
                log.debug("Encuesta NPS cerrada con selector: {}", selector);
                Thread.sleep(2000);
                return true;
            }
        } catch (NoSuchElementException e) {
            log.debug("Selector {} no encontrado: {}", selector, e.getMessage());
        } catch (ElementNotInteractableException e) {
            log.debug("Selector {} no es interactuable: {}", selector, e.getMessage());
        } catch (InterruptedException e) {
            handleInterruptedException(e);
            log.debug("Interrupción con selector {}: {}", selector, e.getMessage());
        } catch (Exception e) {
            log.debug("Error con selector de encuesta {}: {}", selector, e.getMessage());
        }
        return false;
    }

    /**
     * Chequea si hay algun selector que indique que se cerror la sesion exitosamente
     *
     * @param driver maenjador de pagina
     * @param selectors selector a buscar
     * @return {@link boolean}
     */
    private boolean checkAnyLogoutIndicator(WebDriver driver, String[] selectors) {
        for (String selector : selectors) {
            if (checkLogoutWithSelector(driver, selector)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cuerpo de la comparacion de selector que verifica logout
     *
     * @param driver manejador de pagina
     * @param selector selector ubicar
     * @return {@link boolean}
     */
    private boolean checkLogoutWithSelector(WebDriver driver, String selector) {
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector(selector));
            if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                log.debug("Logout verificado con selector: {}", selector);
                return true;
            }
        } catch (NoSuchElementException e) {
            log.debug("Selector de verificacion de logout {} no encontrado: {}", selector, e.getMessage());
        } catch (StaleElementReferenceException e) {
            log.debug("Selector {} obsoleto: {}", selector, e.getMessage());
        } catch (Exception e) {
            log.debug("Selector {} no funcionó para verificar logout: {}", selector, e.getMessage());
        }
        return false;
    }

    private boolean hasMovimientosResults(WebDriver driver) {
        try {
            List<WebElement> filasResultados = driver.findElements(
                    By.xpath("//bcp-table-row-bpbaaa[@index and not(contains(@class, 'header'))]")
            );

            List<WebElement> mensajeSinResultados = driver.findElements(
                    By.xpath("//*[contains(text(), 'No se encontraron resultados') or contains(text(), 'sin resultados')]")
            );

            if (!filasResultados.isEmpty()) {
                log.info("Se encontraron {} movimientos", filasResultados.size());
                return true;
            }

            if (!mensajeSinResultados.isEmpty()) {
                log.info("No se encontraron movimientos para el rango de fechas especificado");
                return false;
            }

            return false;
        } catch (Exception e) {
            log.warn("Error verificando resultados de movimientos: {}", e.getMessage());
            return false;
        }
    }

    private int getTotalPages(WebDriver driver) {
        try {
            WebElement pagination = driver.findElement(By.cssSelector("bcp-pagination-bpbaaa"));
            List<WebElement> pageItems = pagination.findElements(By.cssSelector("li.page"));
            return pageItems.size();
        } catch (Exception e) {
            log.debug("No se encontró paginación, asumiendo 1 página");
            return 1;
        }
    }

    private void navigateToPage(WebDriver driver, int pageNumber) {
        try {
            WebElement pageLink = driver.findElement(By.xpath("//li[@class='page' and contains(., '" + pageNumber + "')]"));
            pageLink.click();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.warn("Error navegando a página {}: {}", pageNumber, e.getMessage());
        }
    }

    private void extractPageData(WebDriver driver, List<Map<String, Object>> movimientos) {
        try {
            List<WebElement> filas = driver.findElements(By.cssSelector("bcp-table-row-bpbaaa[index]"));

            for (WebElement fila : filas) {
                try {
                    List<WebElement> columnas = fila.findElements(By.cssSelector("bcp-table-col-bpbaaa"));

                    if (columnas.size() >= 6) {
                        Map<String, Object> movimiento = new HashMap<>();

                        // Fecha
                        movimiento.put("fecha", getColumnText(columnas.get(0)));

                        // Fecha Valuta
                        movimiento.put("fecha_valor", getColumnText(columnas.get(1)));

                        // Descripción
                        movimiento.put("descripcion", getColumnText(columnas.get(2)));

                        // Número de operación
                        movimiento.put("operacion", getColumnText(columnas.get(3)));

                        // Monto (puede ser positivo o negativo)
                        String montoText = getColumnText(columnas.get(4));
                        double monto = MetodsGeneric.parseSaldo(montoText);
                        movimiento.put("monto", monto);

                        // Determinar si es débito o crédito
                        if (montoText.contains("-")) {
                            movimiento.put("tipo", "DEBITO");
                        } else {
                            movimiento.put("tipo", "CREDITO");
                        }

                        // Saldo
                        String saldoText = getColumnText(columnas.get(4));
                        double saldo = MetodsGeneric.parseSaldo(saldoText);
                        movimiento.put("saldo", saldo);

                        //Referencia
                        movimiento.put("referencia", "-");

                        movimientos.add(movimiento);
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
     *
     * @param columna extrae el valor de la columna enviada como parametro
     *
     * @return {@link String} valor de la columna
     */
    private String getColumnText(WebElement columna) {
        try {
            List<WebElement> paragraphs = columna.findElements(By.cssSelector("bcp-paragraph-bpbaaa"));
            if (!paragraphs.isEmpty()) {
                return paragraphs.get(0).getText().trim();
            }

            return columna.getText().trim();
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
