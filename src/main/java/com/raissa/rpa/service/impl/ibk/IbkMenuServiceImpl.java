package com.raissa.rpa.service.impl.ibk;

import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.exception.IbkException;
import com.raissa.rpa.service.ibk.IbkMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IbkMenuServiceImpl implements IbkMenuService {
    public boolean verifyLoginSuccess(WebDriver driver) {
        log.info("Verificando si el login fue exitoso...");

        try {
            return waitAndVerifyLoginSuccess(driver);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
            return false;
        } catch (Exception e) {
            log.error("Error inesperado verificando login: {}", e.getMessage());
            return false;
        }
    }

    public boolean closeCampaignPopupIfPresent(WebDriver driver) {
        By overlay = By.cssSelector("div.overlay_web");
        By closeButton = By.cssSelector("#popup__cerrar.popup__close_web");

        // Si no hay overlay, no hacemos nada
        if (driver.findElements(overlay).isEmpty()) {
            log.debug("No se encontró popup de campaña");
            return false;
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        try {
            WebElement overlayEl = wait.until(ExpectedConditions.visibilityOfElementLocated(overlay));
            WebElement closeEl = wait.until(ExpectedConditions.elementToBeClickable(closeButton));

            try {
                closeEl.click();
                log.debug("Popup cerrado con click estándar");
            } catch (ElementNotInteractableException e) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", closeEl);
                log.debug("Popup cerrado vía JavaScript (fallback)");
            }

            wait.until(ExpectedConditions.invisibilityOf(overlayEl));
            log.info("Overlay de campaña cerrado correctamente");
            return true;

        } catch (TimeoutException e) {
            log.warn("Timeout esperando cerrar el popup de campaña: {}", e.getMessage());
            return false;
        }
    }

    public boolean clickConsultas(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement consultasHeader = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//nav[contains(@class,'nav-main')]//div[contains(@class,'menu-buttons__button') and .//div[contains(normalize-space(),'Consultas')]]")
            ));
            clickWithFallback(driver, consultasHeader);

            WebElement cuentasPanel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ul[contains(@class,'websidenav-submenu')][@data-test='ulCuentas']")
            ));

            WebElement saldosLink = cuentasPanel.findElement(
                    By.xpath(".//a[contains(@class,'websidenav-submenu__link')][normalize-space()='Saldos']")
            );
            clickWithFallback(driver, saldosLink);

            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public void waitForAccountsToLoad(WebDriver driver) {
        try {
            log.debug("Esperando a que carguen los datos de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ibk-companie-balance//ibk-card-header[normalize-space()='Cuentas']")
            ));

            wait.until(drv -> {
                try {
                    WebElement enterprise = drv.findElement(
                            By.cssSelector("ibk-companie-balance [data-test='lblEnterprise']")
                    );
                    return !enterprise.getText().trim().isEmpty();
                } catch (NoSuchElementException | StaleElementReferenceException ex) {
                    return false;
                }
            });

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("ibk-companie-balance ibk-table[data-test='tblAccount'] ibk-table-row.ng-star-inserted")
            ));
            wait.until(drv -> drv.findElements(
                    By.cssSelector("ibk-companie-balance span[data-test='lblAmount']")
            ).stream().anyMatch(el -> el.getText().matches(".*\\d")));

            Thread.sleep(500);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando carga de cuentas (desktop): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Error esperando carga de cuentas (desktop): {}", e.getMessage());
            throw e;
        }
    }

    public List<Map<String, Object>> extractAccountsData(WebDriver driver) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        try {
            log.debug("Extrayendo datos reales de cuentas (desktop)...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement dataTable = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("ibk-companie-balance ibk-table[data-test='tblAccount']")
            ));

            List<WebElement> accountRows = dataTable.findElements(
                    By.cssSelector("ibk-table-body ibk-table-row.ng-star-inserted")
            );

            log.debug("Encontradas {} filas de cuentas (desktop)", accountRows.size());

            for (WebElement row : accountRows) {
                Map<String, Object> accountData = extractAccountFromRow(row);
                if (!accountData.isEmpty()) {
                    accounts.add(accountData);
                    log.debug("Cuenta extraída: {}", accountData.get(Constantes.KEY_NUMERO_CUENTA));
                }
            }

            return accounts;

        } catch (TimeoutException e) {
            log.error("Timeout localizando tabla de cuentas: {}", e.getMessage());
            return accounts;
        } catch (Exception e) {
            log.error("Error extrayendo datos de cuentas: {}", e.getMessage());
            return accounts;
        }
    }

    public boolean clickMovimientos(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        try {
            WebElement consultasHeader = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//nav[contains(@class,'nav-main')]//div[contains(@class,'menu-buttons__button') and .//div[contains(normalize-space(),'Consultas')]]")
            ));
            clickWithFallback(driver, consultasHeader);

            WebElement cuentasPanel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ul[contains(@class,'websidenav-submenu')][@data-test='ulCuentas']")
            ));

            WebElement movimientosLink = cuentasPanel.findElement(
                    By.xpath(".//a[contains(@class,'websidenav-submenu__link')][normalize-space()='Movimientos']")
            );
            clickWithFallback(driver, movimientosLink);

            return true;
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    public void waitForMovementsToLoad(WebDriver driver) {
        try {
            log.debug("Esperando a que carguen los datos de movimientos...");

            MetodsGeneric.randomWait(2000,4000);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//ibk-history-filter//ibk-headline-title[normalize-space()='Movimientos']")
            ));
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        } catch (TimeoutException e) {
            log.warn("Timeout esperando carga de movimietnos de cuentas (desktop): {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Error esperando carga de movimientos de cuentas (desktop): {}", e.getMessage());
            throw e;
        }
    }

    public boolean selectCuenta(WebDriver driver, String numeroCuenta) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            String displayedAccount = formatAccountForDropdown(numeroCuenta);

            // 1) Abrir el combo de cuentas si está colapsado.
            WebElement selectTrigger = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("mat-select[data-test='cmbAccount']")
            ));
            if ("false".equalsIgnoreCase(selectTrigger.getDomAttribute("aria-expanded"))) {
                selectTrigger.click();
            }

            // 2) Esperar el panel de opciones.
            By panelLocator = By.cssSelector("div.mat-mdc-select-panel.mdc-menu-surface--open");
            WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(panelLocator));

            // 3) Encontrar la opción cuyo texto contiene el número de cuenta con guion.
            By optionLocator = By.xpath(
                    ".//mat-option//div[contains(@class,'ibk-label-account-select')][contains(normalize-space(), '" + displayedAccount + "')]"
            );
            WebElement option = wait.until(ExpectedConditions.elementToBeClickable(panel.findElement(optionLocator)));

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", option);
            option.click();

            // 4) Esperar a que el panel se cierre.
            wait.until(ExpectedConditions.invisibilityOf(panel));

            MetodsGeneric.randomWait(2000, 3000);

            log.debug("Cuenta {} seleccionada correctamente", numeroCuenta);
            return true;

        } catch (TimeoutException e) {
            log.warn("Timeout seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        }
    }

    public boolean setDateRange(WebDriver driver, String fechaInicio, String fechaFin) {
        log.info("Iniciando carga de rango de fechas (histórico)");
        try {
            MetodsGeneric.randomWait(2000, 3000);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

            // 1) Esperar el contenedor del datepicker
            WebElement rangeContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("ibk-historical-date ibk-datepicker-range-v2")
            ));

            // 2) Inputs desde/hasta (identificados por data-mat-calendar)
            List<WebElement> visibleInputs = wait.until(drv -> {
                List<WebElement> inputs = rangeContainer.findElements(
                        By.cssSelector("input.mat-datepicker-input[data-mat-calendar]")
                );
                List<WebElement> displayed = inputs.stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());
                return displayed.size() >= 2 ? displayed : null;
            });

            WebElement fechaInicioInput = visibleInputs.get(0);
            WebElement fechaFinInput = visibleInputs.get(1);

            // 3) Setear valores via JS y disparar eventos
            JavascriptExecutor js = (JavascriptExecutor) driver;
            setInputValue(js, fechaInicioInput, fechaInicio);
            setInputValue(js, fechaFinInput, fechaFin);

            MetodsGeneric.randomWait(500, 800);

            // 4) Opcional: verificar que los inputs reflejan el valor
            if (!fechaInicio.equals(fechaInicioInput.getAttribute("value"))) {
                log.warn("El campo inicio no reflejó el valor esperado: {}", fechaInicioInput.getAttribute("value"));
            }
            if (!fechaFin.equals(fechaFinInput.getAttribute("value"))) {
                log.warn("El campo fin no reflejó el valor esperado: {}", fechaFinInput.getAttribute("value"));
            }

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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            // 1) Localizar el botón “Buscar” dentro del filtro histórico.
            WebElement buscarBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("ibk-history-filter button[data-test='btnSearch']")
            ));

            // 2) Asegurarnos de que el botón sea interactuable.
            wait.until(ExpectedConditions.elementToBeClickable(buscarBtn));
            scrollIntoView(driver, buscarBtn);

            if (isButtonDisabled(buscarBtn)) {
                log.warn("El botón de búsqueda está deshabilitado, validando requisitos…");
                driver.findElement(By.tagName("body")).click();
                MetodsGeneric.randomWait(500, 800);

                if (isButtonDisabled(buscarBtn)) {
                    throw new IbkException("Botón de búsqueda permanece deshabilitado después de fijar el rango",
                            "IBK_SEARCH_DISABLED",
                            "Verifica que las fechas y filtros cumplan los requisitos");
                }
            }

            // 3) Click con fallback (algunos mat-button ignoran el click clásico si el overlay se monta encima).
            try {
                buscarBtn.click();
            } catch (Exception e) {
                new Actions(driver).moveToElement(buscarBtn).click().perform();
            }

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

    public boolean waitForMovimientosToLoad(WebDriver driver) {
        log.debug("Esperando a que cargue la grilla de movimientos…");

        try {
            MetodsGeneric.randomWait(1000,2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        By desktopRows = By.cssSelector("ibk-table[data-test='tblResult'] ibk-table-row.ng-star-inserted");
        By emptyState = By.xpath("//*[contains(normalize-space(),'No se encontraron resultados') or contains(normalize-space(),'sin resultados')]");

        ExpectedCondition<Boolean> criterioCarga = drv -> {
            try {
                if (!drv.findElements(desktopRows).isEmpty()) {
                    log.debug("Tabla desktop detectada con {} filas",
                            drv.findElements(desktopRows).size());
                    return true;
                }
                if (!drv.findElements(emptyState).isEmpty()) {
                    log.debug("Mensaje de resultados vacíos detectado");
                    return true;
                }
                return false;
            } catch (StaleElementReferenceException ignored) {
                return false;
            }
        };

        try {
            wait.until(criterioCarga);

            boolean hayRegistros = !driver.findElements(desktopRows).isEmpty();
            boolean sinResultados = !driver.findElements(emptyState).isEmpty();

            if (hayRegistros) {
                log.debug("La tabla contiene movimientos");
                return true;
            }
            if (sinResultados) {
                log.debug("Se mostró mensaje de sin resultados");
            } else {
                log.debug("No se detectaron registros ni mensaje de vacío");
            }
            return false;

        } catch (TimeoutException e) {
            log.warn("Timeout esperando la grilla de movimientos: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> extractMovimientosData(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();

        try {
            if (!waitForMovimientosToLoad(driver)) {
                log.info("No se encontraron movimientos");
                return movimientos;
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("ibk-history-filter")
            ));

            movimientos.addAll(extractDesktopMovimientos(driver));
        } catch (Exception e) {
            log.warn("Error extrayendo datos de movimientos: {}", e.getMessage(), e);
        }

        return movimientos;
    }

    public void openProfileDropdown(WebDriver driver) {
        try {
            log.debug("Buscando trigger del menú de usuario (desktop)…");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

            WebElement trigger = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("ibk-user .mat-mdc-menu-trigger.action-menu")
            ));

            if (!"true".equalsIgnoreCase(trigger.getAttribute("aria-expanded"))) {
                clickWithFallback(driver, trigger);
            }

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.cdk-overlay-pane .user-menu[data-test='lstPrimary']")
            ));

            log.debug("Menú de usuario desplegado correctamente");
        } catch (TimeoutException e) {
            log.error("No se pudo abrir el menú de usuario: {}", e.getMessage());
            throw new BcpException("No se pudo abrir el menú del usuario",
                    "IBK_MENU_OPEN_ERROR",
                    "Revisa si la cabecera cargó correctamente");
        }
    }

    public void clickLogoutButton(WebDriver driver) {
        try {
            log.debug("Buscando opción 'Cerrar sesión' en el menú…");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

            WebElement logoutOption = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'mat-mdc-menu-panel')]//a[contains(@class,'user-menu__link')][normalize-space()='Cerrar sesión']")
            ));

            clickWithFallback(driver, logoutOption);

            try {
                MetodsGeneric.randomWait(1000,2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (TimeoutException e) {
            log.error("No se pudo completar el logout: {}", e.getMessage());
            throw new BcpException("No se encontró el botón de cerrar sesión",
                    "IBK_LOGOUT_CLICK_ERROR",
                    "Verifica que el menú esté desplegado y que el diálogo de confirmación aparezca");
        }
    }

    public void handleNpsSurvey(WebDriver driver) {
        try {
            log.debug("Opción 'Cerrar sesión' clickeada, esperando diálogo de confirmación…");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

            WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("mat-dialog-container.mat-mdc-dialog-container")
            ));

            WebElement continuarBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[data-test='btnContinuarAlert']")
            ));

            clickWithFallback(driver, continuarBtn);

            try {
                MetodsGeneric.randomWait(2000,3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            wait.until(ExpectedConditions.invisibilityOf(dialog));

            log.debug("Confirmación de logout aceptada");

        } catch (TimeoutException e) {
            log.error("No se pudo completar el logout, no hay dialogo: {}", e.getMessage());
            throw new BcpException("No se encontró modal de cerrar sesión",
                    "IBK_LOGOUT_CLICK_ERROR",
                    "Verifica que el diálogo de confirmación aparezca");
        }
    }

    public boolean verifyLogoutSuccess(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ibk-auth-main")),
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[data-test='btnEnter']")),
                    ExpectedConditions.urlContains("/login")
            ));

            boolean loginVisible = driver.findElements(By.cssSelector("button[data-test='btnEnter']")).stream()
                    .anyMatch(WebElement::isDisplayed);

            if (loginVisible) {
                log.debug("Logout verificado: botón 'Iniciar sesión' visible");
                return true;
            }

            String url = driver.getCurrentUrl();
            if (url != null && (url.contains("/login") || url.contains("/ciam") || url.contains("/auth"))) {
                log.debug("Logout inferido por URL de login: {}", url);
                return true;
            }

            log.warn("No se pudo confirmar el logout de forma explícita");
            return false;

        } catch (TimeoutException e) {
            log.warn("Timeout esperando la pantalla de login tras logout: {}", e.getMessage());
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

        log.debug("Indicadores login - Menu: {}", menuVisible);

        return menuVisible;
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
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ibk-menu-sidebar-desktop"))
            );

            boolean isMenuVisible = sideMenu.isDisplayed();
            log.info("Menú lateral encontrado y visible: {}", isMenuVisible);

            return isMenuVisible;

        } catch (Exception e) {
            log.warn("No se pudo encontrar/verificar el menú lateral: {}", e.getMessage());
            return false;
        }
    }

    private void clickWithFallback(WebDriver driver, WebElement element) {
        try {
            element.click();
        } catch (Exception clickFailed) {
            new Actions(driver).moveToElement(element).click().perform();
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
            // 1. Número de cuenta (3ª columna)
            WebElement accountCell = row.findElement(By.cssSelector("ibk-table-cell:nth-of-type(3)"));
            String rawAccountText = accountCell.getText().trim();
            String numeroCuenta = extractAccountNumber(rawAccountText);
            numeroCuenta = MetodsGeneric.cleanAccountNumber(numeroCuenta);
            if (numeroCuenta.isEmpty()) {
                log.warn("Fila sin número de cuenta válido: {}", rawAccountText);
                return Collections.emptyMap();
            }
            account.put(Constantes.KEY_NUMERO_CUENTA, numeroCuenta);

            // 2. Moneda (2ª columna) -> símbolo S/ o $
            WebElement currencyCell = row.findElement(By.cssSelector("ibk-table-cell:nth-of-type(2)"));
            String currencyText = currencyCell.getText().replaceAll("\\s+", "");
            account.put(Constantes.KEY_MONEDA, currencyText.equals("S/") ? "PEN" : "USD");

            // 3. Saldo contable (5ª columna)
            WebElement amountContableElement = row.findElement(By.cssSelector(
                    "ibk-table-cell:nth-of-type(5) span[data-test='lblAmount']"
            ));
            String amountContableText = amountContableElement.getText().trim();
            double saldoContable = MetodsGeneric.parseSaldo(amountContableText);
            account.put(Constantes.KEY_SALDO_CONT, saldoContable);

            // 4. Saldo disponible (6ª columna)
            WebElement amountDisponibleElement = row.findElement(By.cssSelector(
                    "ibk-table-cell:nth-of-type(6) span[data-test='lblAmount']"
            ));
            String amountDisponibleText = amountDisponibleElement.getText().trim();
            double saldoDisponible = MetodsGeneric.parseSaldo(amountDisponibleText);
            account.put(Constantes.KEY_SALDO_DISP, saldoDisponible);

            return account;

        } catch (NoSuchElementException e) {
            log.warn("Elementos esperados no encontrados en fila: {}", e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error extrayendo datos de fila: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String extractAccountNumber(String cellText) {
        // Preferimos el formato 200-3002628315; si no, devolvemos el texto completo.
        Pattern pattern = Pattern.compile("\\b\\d{3}-\\d{9,}\\b");
        Matcher matcher = pattern.matcher(cellText);
        return matcher.find() ? matcher.group() : cellText;
    }

    private String formatAccountForDropdown(String numeroCuenta) {
        String clean = numeroCuenta.replaceAll("\\D+", ""); // quita cualquier separador si viene mezclado
        if (clean.length() > 3) {
            return clean.substring(0, 3) + "-" + clean.substring(3);
        }
        return clean;
    }

    private boolean isButtonDisabled(WebElement button) {
        return !button.isEnabled()
                || Boolean.parseBoolean(button.getAttribute("disabled"))
                || button.getAttribute("class").toLowerCase().contains("mdc-button--disabled");
    }

    private void scrollIntoView(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});", element
        );
    }

    private void setInputValue(JavascriptExecutor js, WebElement input, String value) {
        js.executeScript("arguments[0].value = arguments[1];", input, value);
        js.executeScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", input);
        js.executeScript("arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", input);
        js.executeScript("arguments[0].dispatchEvent(new Event('blur', { bubbles: true }));", input);
    }

    private List<Map<String, Object>> extractDesktopMovimientos(WebDriver driver) {
        List<Map<String, Object>> movimientos = new ArrayList<>();

        List<WebElement> filas = driver.findElements(
                By.cssSelector("ibk-table[data-test='tblResult'] ibk-table-body ibk-table-row.ng-star-inserted")
        );

        if (filas.isEmpty()) {
            log.debug("Tabla de movimientos desktop sin filas visibles");
            return movimientos;
        }

        for (WebElement fila : filas) {
            try {
                List<WebElement> columnas = fila.findElements(By.cssSelector("ibk-table-cell"));

                if (columnas.size() < 8) {
                    log.warn("Fila con columnas insuficientes: {}", columnas.size());
                    continue;
                }

                Map<String, Object> movimiento = new HashMap<>();
                movimiento.put("fecha", normalizeText(columnas.get(0)));
                movimiento.put("fecha_valor", normalizeText(columnas.get(1)));
                movimiento.put("operacion", normalizeText(columnas.get(2)));

                String movimientoLabel = normalizeText(columnas.get(3));
                String descripcionLabel = normalizeText(columnas.get(4));
                String descripcion = buildDescripcion(movimientoLabel, descripcionLabel);
                movimiento.put("descripcion", descripcion);

                movimiento.put("referencia", "-");

                double monto = parseAmountColumn(columnas.get(6));
                movimiento.put("monto", monto);
                movimiento.put("tipo", monto < 0 ? "DEBITO" : "CREDITO");

                double saldo = parseAmountColumn(columnas.get(7));
                movimiento.put("saldo", saldo);

                movimientos.add(movimiento);

            } catch (Exception ex) {
                log.warn("Error procesando fila de movimientos: {}", ex.getMessage());
            }
        }

        log.debug("Movimientos (desktop) extraídos: {}", movimientos.size());
        return movimientos;
    }

    private String normalizeText(WebElement cell) {
        String text = cell.getText();
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String buildDescripcion(String movimiento, String detalle) {
        if (movimiento.isEmpty()) {
            return detalle;
        }
        if (detalle.isEmpty() || detalle.equals("-")) {
            return movimiento;
        }
        return movimiento + " - " + detalle;
    }

    private double parseAmountColumn(WebElement cell) {
        try {
            WebElement amountSpan = cell.findElement(By.cssSelector("span.label-balance__amount"));
            String amountText = amountSpan.getText().trim();
            return MetodsGeneric.parseSaldo(amountText);
        } catch (NoSuchElementException ignored) {
            return MetodsGeneric.parseSaldo(cell.getText().trim());
        }
    }

    private String textOrEmpty(WebElement context, String css) {
        return context.findElements(By.cssSelector(css)).stream()
                .findFirst()
                .map(el -> el.getText().replaceAll("\\s+", " ").trim())
                .orElse("");
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
                "IBK_NAVIGATION_INTERRUPTED",
                "El proceso fue interrumpido durante la navegación");
    }
}
