package com.raissa.rpa.service.impl.ibk;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.raissa.rpa.exception.IbkException;
import com.raissa.rpa.service.ibk.IbkMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IbkMenuServiceImpl implements IbkMenuService {
    public boolean verifyLoginSuccess(Page page) {
        log.info("Verificando si el login fue exitoso...");

        try {
            log.debug("Esperando estabilización de la página post-login...");
            MetodsGeneric.randomWaitPage(page, 2_500, 3_500);

            Locator sideMenu = MetodsGeneric.waitForAttached(page, "ibk-menu-sidebar-desktop", 5_000);

            boolean exists = sideMenu.count() > 0;
            log.info("Menú lateral encontrado y visible: {}", exists);

            return exists;
        } catch (TimeoutError e) {
            log.error("Timeout esperando que cargue la pagina de IBK: {}", e.getMessage(), e);
            throw new IbkException("Timeout",
                    "IBK_TIMEOUT",
                    "No se cargo la pagina en el tiempo esperado");
        } catch (Exception e) {
            log.error("Error inesperado verificando login: {}", e.getMessage());
            return false;
        }
    }

    public boolean closeCampaignPopupIfPresent(Page page) {
        String overlaySelector = "div.overlay_web";
        String closeButtonSelector = "#popup__cerrar.popup__close_web";
        int timeoutMs = 5_000;

        try {
            if (page.locator(overlaySelector).count() == 0) {
                log.debug("No se encontró popup de campaña");
                return false;
            }

            Locator overlayEl = MetodsGeneric.waitForAttached(page, overlaySelector, timeoutMs);
            if (!overlayEl.isVisible()) {
                log.debug("Overlay existe pero no está visible");
                return false;
            }

            Locator closeEl = MetodsGeneric.waitForVisible(page, closeButtonSelector, timeoutMs);

            MetodsGeneric.clickWithFallback(page, closeEl, timeoutMs);
            log.debug("Popup cerrado (click estándar o fallback JS)");

            MetodsGeneric.waitForHidden(overlayEl, timeoutMs);

            log.info("Overlay de campaña cerrado correctamente");
            return true;

        } catch (Exception e) {
            log.warn("Error esperando el popup de campaña: {}", e.getMessage());
            return false;
        }
    }

    public boolean clickConsultas(Page page) {
        int timeoutMs = 5_000;
        try {
            Locator saldosLink = MetodsGeneric.waitForAttached(
                    page,
                    "a[data-test='lnksaldos']",
                    timeoutMs
            );

            MetodsGeneric.clickJsFallback(page, saldosLink);

            page.waitForURL("**/consultas/saldos", new Page.WaitForURLOptions().setTimeout(timeoutMs));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Map<String, Object>> extractAccountsData(Page page) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        try {
            log.debug("Extrayendo datos reales de cuentas (desktop)...");
            int timeoutMs = 15_000;

            Locator dataTable = MetodsGeneric.waitForVisible(
                    page,
                    "ibk-companie-balance ibk-table[data-test='tblAccount']",
                    timeoutMs
            );

            Locator accountRows = dataTable.locator("ibk-table-body ibk-table-row.ng-star-inserted");
            int rowCount = accountRows.count();

            log.debug("Encontradas {} filas de cuentas (desktop)", rowCount);

            for (int i = 0; i < rowCount; i++) {
                Locator row = accountRows.nth(i);
                Map<String, Object> accountData = extractAccountFromRow(row);

                if (!accountData.isEmpty()) {
                    accounts.add(accountData);
                    log.debug("Cuenta extraída: {}", accountData.get(Constantes.KEY_NUMERO_CUENTA));
                }
            }

            return accounts;

        } catch (TimeoutError e) {
            log.error("Timeout localizando tabla de cuentas: {}", e.getMessage());
            return accounts;
        } catch (Exception e) {
            log.error("Error extrayendo datos de cuentas: {}", e.getMessage());
            return accounts;
        }
    }

    public boolean clickMovimientos(Page page) {
        int timeoutMs = 5_000;

        try {
            Locator movimientosLink = MetodsGeneric.waitForAttached(
                    page,
                    "a[data-test='lnkmovimientos']",
                    timeoutMs
            );

            MetodsGeneric.clickJsFallback(page, movimientosLink);

            if (!closeCampaignPopupIfPresent(page)) {
                log.info("No hay popup que cerrar");
            }

            page.waitForURL("**/consultas/movimientos/historial/",
                    new Page.WaitForURLOptions().setTimeout(timeoutMs));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean selectCuenta(Page page, String numeroCuenta) {
        try {
            int timeoutMs = 5_000;
            String displayedAccount = formatAccountForDropdown(numeroCuenta);

            // 1) Abrir el combo de cuentas si está colapsado.
            Locator selectTrigger = MetodsGeneric.waitForVisible(
                    page,
                    "mat-select[data-test='cmbAccount']",
                    timeoutMs
            );
            String expanded = selectTrigger.getAttribute("aria-expanded");
            if (!"true".equalsIgnoreCase(expanded)) {
                MetodsGeneric.clickWithFallback(page, selectTrigger, timeoutMs);
            }

            // 2) Esperar el panel de opciones.
            Locator panel = MetodsGeneric.waitForVisible(
                    page,
                    "div.mat-mdc-select-panel.mdc-menu-surface--open",
                    timeoutMs
            );

            // 3) Encontrar la opción cuyo texto contiene el número de cuenta con guion.
            Locator option = panel
                    .locator("mat-option .ibk-label-account-select")
                    .filter(new Locator.FilterOptions().setHasText(displayedAccount))
                    .first();

            option.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeoutMs));

            option.scrollIntoViewIfNeeded();
            MetodsGeneric.clickWithFallback(page, option, timeoutMs);

            // 4) Esperar a que el panel se cierre.
            MetodsGeneric.waitForHidden(panel, timeoutMs);

            MetodsGeneric.randomWaitPage(page, 2_000, 3_000);

            log.debug("Cuenta {} seleccionada correctamente", numeroCuenta);
            return true;
        } catch (Exception e) {
            log.warn("Error seleccionando cuenta {}: {}", numeroCuenta, e.getMessage());
            return false;
        }
    }

    public boolean setDateRange(Page page, String fechaInicio, String fechaFin) {
        log.info("Iniciando carga de rango de fechas (histórico)");
        try {
            MetodsGeneric.randomWaitPage(page, 2_000, 3_000);
            int timeoutMs = 8_000;

            // 1) Esperar el contenedor del datepicker
            Locator rangeContainer = MetodsGeneric.waitForVisible(
                    page,
                    "ibk-historical-date ibk-datepicker-range-v2",
                    timeoutMs
            );

            // 2) Inputs desde/hasta (identificados por data-mat-calendar)
            Locator inputs = rangeContainer.locator("input.mat-datepicker-input[data-mat-calendar]");
            long end = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < end) {
                if (inputs.count() >= 2) break;
                page.waitForTimeout(150);
            }
            if (inputs.count() < 2) {
                throw new RuntimeException("No se encontraron al menos 2 inputs de fecha");
            }

            List<Locator> visibles = new ArrayList<>();
            int count = inputs.count();
            for (int i = 0; i < count; i++) {
                Locator in = inputs.nth(i);
                if (in.isVisible()) visibles.add(in);
            }

            if (visibles.size() < 2) {
                log.warn("No se encontraron 2 inputs visibles para rango de fechas");
                return false;
            }

            Locator fechaInicioInput = visibles.get(0);
            Locator fechaFinInput = visibles.get(1);

            // 3) Set por JS + eventos
            setInputValue(fechaInicioInput, fechaInicio);
            setInputValue(fechaFinInput, fechaFin);

            MetodsGeneric.randomWaitPage(page, 500, 800);

            // 4) Verificación opcional
            String valueIni = fechaInicioInput.inputValue();
            if (!fechaInicio.equals(valueIni)) {
                log.warn("El campo inicio no reflejó el valor esperado: {}", valueIni);
            }

            String valueFin = fechaFinInput.inputValue();
            if (!fechaFin.equals(valueFin)) {
                log.warn("El campo fin no reflejó el valor esperado: {}", valueFin);
            }

            return true;
        } catch (Exception e) {
            log.warn("Error configurando rango de fechas {} - {}: {}", fechaInicio, fechaFin, e.getMessage());
            return false;
        }
    }

    public void applyFilters(Page page) {
        try {
            int timeoutMs = 5_000;

            // 1) Localizar el botón “Buscar” dentro del filtro histórico.
            Locator buscarBtn = MetodsGeneric.waitForAttached(
                    page,
                    "ibk-history-filter button[data-test='btnSearch']",
                    timeoutMs
            );

            // 2) Llevar a vista y validar habilitado
            buscarBtn.scrollIntoViewIfNeeded();

            if (isButtonDisabled(buscarBtn)) {
                log.warn("El botón de búsqueda está deshabilitado, validando requisitos…");
                page.locator("body").first().click();
                MetodsGeneric.randomWaitPage(page, 500, 800);

                if (isButtonDisabled(buscarBtn)) {
                    throw new IbkException(
                            "Botón de búsqueda permanece deshabilitado después de fijar el rango",
                            "IBK_SEARCH_DISABLED",
                            "Verifica que las fechas y filtros cumplan los requisitos"
                    );
                }
            }

            // 3) Click con fallback
            MetodsGeneric.clickWithFallback(page, buscarBtn, timeoutMs);
            log.info("Búsqueda de movimientos ejecutada exitosamente");
        } catch (Exception e) {
            log.warn("Error aplicando filtros de búsqueda: {}", e.getMessage());
            throw new IbkException("No se pudo ejecutar la búsqueda");
        }
    }

    public boolean waitForMovimientosToLoad(Page page) {
        log.debug("Esperando a que cargue la grilla de movimientos…");

        MetodsGeneric.randomWaitPage(page, 500, 1_000);
        int timeoutMs = 5_000;

        String desktopRowsSel = "ibk-table[data-test='tblResult'] ibk-table-row.ng-star-inserted";
        String emptyStateXpath = "//*[contains(normalize-space(),'No se encontraron resultados') or contains(normalize-space(),'sin resultados')]";

        try {
            page.waitForFunction(
                    "([rowsSel, emptyXp]) => {" +
                            " const rows = document.querySelectorAll(rowsSel);" +
                            " if (rows.length > 0) return true;" +
                            " const empty = document.evaluate(emptyXp, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);" +
                            " return empty.snapshotLength > 0;" +
                            "}",
                    new Object[]{desktopRowsSel, emptyStateXpath},
                    new Page.WaitForFunctionOptions().setTimeout(timeoutMs)
            );

            int rows = page.locator(desktopRowsSel).count();
            int empty = page.locator("xpath=" + emptyStateXpath).count();

            if (rows > 0) {
                log.debug("La tabla contiene movimientos");
                return true;
            }
            if (empty > 0) {
                log.debug("Se mostró mensaje de sin resultados");
            } else {
                log.debug("No se detectaron registros ni mensaje de vacío");
            }
            return false;

        } catch (Exception e) {
            log.warn("Timeout/error esperando la grilla de movimientos: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> extractMovimientosData(Page page) {
        List<Map<String, Object>> movimientos = new ArrayList<>();

        try {
            if (!waitForMovimientosToLoad(page)) {
                log.info("No se encontraron movimientos");
                return movimientos;
            }

            MetodsGeneric.waitForAttached(page, "ibk-history-filter", 5_000);

            movimientos.addAll(extractDesktopMovimientos(page));
        } catch (Exception e) {
            log.warn("Error extrayendo datos de movimientos: {}", e.getMessage(), e);
        }

        return movimientos;
    }

    public void openProfileDropdown(Page page) {
        try {
            int timeoutMs = 8_000;
            log.debug("Buscando trigger del menú de usuario (desktop)…");

            Locator trigger = MetodsGeneric.waitForVisible(
                    page,
                    "ibk-user .mat-mdc-menu-trigger.action-menu",
                    timeoutMs
            );

            String expanded = trigger.getAttribute("aria-expanded");
            if (!"true".equalsIgnoreCase(expanded)) {
                MetodsGeneric.clickWithFallback(page, trigger, timeoutMs);
            }

            MetodsGeneric.waitForVisible(
                    page,
                    "div.cdk-overlay-pane .user-menu[data-test='lstPrimary']",
                    timeoutMs
            );

            log.debug("Menú de usuario desplegado correctamente");
        } catch (Exception e) {
            log.error("No se pudo abrir el menú de usuario: {}", e.getMessage());
            throw new IbkException("No se pudo abrir el menú del usuario",
                    "IBK_MENU_OPEN_ERROR",
                    "Revisa si la cabecera cargó correctamente");
        }
    }

    public void clickLogoutButton(Page page) {
        try {
            log.debug("Buscando opción 'Cerrar sesión' en el menú…");

            int timeoutMs = 8_000;

            Locator logoutOption = MetodsGeneric.waitForVisible(
                    page,
                    "//div[contains(@class,'mat-mdc-menu-panel')]//a[contains(@class,'user-menu__link')][normalize-space()='Cerrar sesión']",
                    timeoutMs
            );

            MetodsGeneric.clickWithFallback(page, logoutOption, timeoutMs);

            MetodsGeneric.randomWaitPage(page, 1_000, 2_000);

        } catch (Exception e) {
            log.error("No se pudo completar el logout: {}", e.getMessage());
            throw new IbkException("No se encontró el botón de cerrar sesión",
                    "IBK_LOGOUT_CLICK_ERROR",
                    "Verifica que el menú esté desplegado y que el diálogo de confirmación aparezca");
        }
    }

    public void handleNpsSurvey(Page page) {
        try {
            log.debug("Opción 'Cerrar sesión' clickeada, esperando diálogo de confirmación…");
            int timeoutMs = 8_000;

            Locator dialog = MetodsGeneric.waitForVisible(
                    page,
                    "mat-dialog-container.mat-mdc-dialog-container",
                    timeoutMs
            );

            Locator continuarBtn = MetodsGeneric.waitForVisible(
                    page,
                    "button[data-test='btnContinuarAlert']",
                    timeoutMs
            );

            MetodsGeneric.clickWithFallback(page, continuarBtn, timeoutMs);
            MetodsGeneric.randomWaitPage(page, 2_000, 3_000);

            dialog.waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                    .setTimeout(timeoutMs));

            log.debug("Confirmación de logout aceptada");

        } catch (Exception e) {
            log.error("No se pudo completar el logout, no hay dialogo: {}", e.getMessage());
            throw new IbkException("No se encontró modal de cerrar sesión",
                    "IBK_LOGOUT_CLICK_ERROR",
                    "Verifica que el diálogo de confirmación aparezca");
        }
    }

    public boolean verifyLogoutSuccess(Page page) {
        try {
            int timeoutMs = 15_000;

            page.waitForFunction(
                    "() => {" +
                            "const authMain = document.querySelector('ibk-auth-main');" +
                            "const btnEnter = document.querySelector(\"button[data-test='btnEnter']\");" +
                            "const href = window.location.href || '';" +
                            "const visible = (el) => !!el && !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);" +
                            "return visible(authMain) || visible(btnEnter) || href.includes('/login') || href.includes('/ciam') || href.includes('/auth');" +
                            "}",
                    new Page.WaitForFunctionOptions().setTimeout(timeoutMs)
            );

            Locator loginBtn = page.locator("button[data-test='btnEnter']").first();
            if (loginBtn.count() > 0 && loginBtn.isVisible()) {
                log.debug("Logout verificado: botón 'Iniciar sesión' visible");
                return true;
            }

            String url = page.url();
            if (url != null && (url.contains("/login") || url.contains("/ciam") || url.contains("/auth"))) {
                log.debug("Logout inferido por URL de login: {}", url);
                return true;
            }

            log.warn("No se pudo confirmar el logout de forma explícita");
            return false;
        } catch (Exception e) {
            log.warn("Timeout esperando la pantalla de login tras logout: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae saldos y cuentas fila a fila
     * @param row fila
     * @return {@link Map} datos de una cuenta por fila
     */
    private Map<String, Object> extractAccountFromRow(Locator row) {
        Map<String, Object> account = new HashMap<>();

        try {
            // 1. Número de cuenta (3ª columna)
            Locator accountCell = row.locator("ibk-table-cell:nth-of-type(3)").first();
            String rawAccountText = Optional.ofNullable(accountCell.textContent()).orElse("").trim();

            String numeroCuenta = extractAccountNumber(rawAccountText);
            numeroCuenta = MetodsGeneric.cleanAccountNumber(numeroCuenta);
            if (numeroCuenta.isEmpty()) {
                log.warn("Fila sin número de cuenta válido: {}", rawAccountText);
                return Collections.emptyMap();
            }
            account.put(Constantes.KEY_NUMERO_CUENTA, numeroCuenta);

            // 2. Moneda (2ª columna) -> símbolo S/ o $
            Locator currencyCell = row.locator("ibk-table-cell:nth-of-type(2)").first();
            String currencyText = Optional.ofNullable(currencyCell.textContent()).orElse("").replaceAll("\\s+", "");
            account.put(Constantes.KEY_MONEDA, "S/".equals(currencyText) ? "PEN" : "USD");

            // 3. Saldo contable (5ª columna)
            Locator amountContableElement = row.locator(
                    "ibk-table-cell:nth-of-type(5) span[data-test='lblAmount']"
            ).first();
            String amountContableText = Optional.ofNullable(amountContableElement.textContent()).orElse("").trim();
            double saldoContable = MetodsGeneric.parseSaldo(amountContableText);
            account.put(Constantes.KEY_SALDO_CONT, saldoContable);

            // 4. Saldo disponible (6ª columna)
            Locator amountDisponibleElement = row.locator(
                    "ibk-table-cell:nth-of-type(6) span[data-test='lblAmount']"
            ).first();
            String amountDisponibleText = Optional.ofNullable(amountDisponibleElement.textContent()).orElse("").trim();
            double saldoDisponible = MetodsGeneric.parseSaldo(amountDisponibleText);
            account.put(Constantes.KEY_SALDO_DISP, saldoDisponible);

            return account;
        } catch (Exception e) {
            log.error("Error extrayendo datos de fila: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Extrae valor numerico del texto de la pagina
     * @param cellText Texto con monto
     * @return {@link String}
     */
    private String extractAccountNumber(String cellText) {
        // Preferimos el formato 200-3002628315; si no, devolvemos el texto completo.
        Pattern pattern = Pattern.compile("\\b\\d{3}-\\d{9,}\\b");
        Matcher matcher = pattern.matcher(cellText);
        return matcher.find() ? matcher.group() : cellText;
    }

    /**
     * Formatea cuenta para seleccion en dropdown
     * @param numeroCuenta numero de cuenta
     * @return {@link String}
     */
    private String formatAccountForDropdown(String numeroCuenta) {
        String clean = numeroCuenta.replaceAll("\\D+", ""); // quita cualquier separador si viene mezclado
        if (clean.length() > 3) {
            return clean.substring(0, 3) + "-" + clean.substring(3);
        }
        return clean;
    }

    /**
     * Extrae los movimientos de la pagina
     * @param page manejador de pagina
     * @return {@link List}
     */
    private List<Map<String, Object>> extractDesktopMovimientos(Page page) {
        List<Map<String, Object>> movimientos = new ArrayList<>();

        Locator filas = page.locator("ibk-table[data-test='tblResult'] ibk-table-body ibk-table-row.ng-star-inserted");
        int totalFilas = filas.count();

        if (totalFilas == 0) {
            log.debug("Tabla de movimientos desktop sin filas visibles");
            return movimientos;
        }

        for (int i = 0; i < totalFilas; i++) {
            try {
                Locator fila = filas.nth(i);
                Locator columnas = fila.locator("ibk-table-cell");
                int nCols = columnas.count();

                if (nCols < 8) {
                    log.warn("Fila con columnas insuficientes: {}", nCols);
                    continue;
                }

                Map<String, Object> movimiento = new HashMap<>();
                movimiento.put("fecha", normalizeText(columnas.nth(0)));
                movimiento.put("fecha_valor", normalizeText(columnas.nth(1)));
                movimiento.put("operacion", normalizeText(columnas.nth(2)));

                String movimientoLabel = normalizeText(columnas.nth(3));
                String descripcionLabel = normalizeText(columnas.nth(4));
                String descripcion = buildDescripcion(movimientoLabel, descripcionLabel);
                movimiento.put("descripcion", descripcion);

                movimiento.put("referencia", "-");

                double monto = parseAmountColumn(columnas.nth(6));
                movimiento.put("monto", monto);
                movimiento.put("tipo", monto < 0 ? "DEBITO" : "CREDITO");

                double saldo = parseAmountColumn(columnas.nth(7));
                movimiento.put("saldo", saldo);

                movimientos.add(movimiento);

            } catch (Exception ex) {
                log.warn("Error procesando fila de movimientos: {}", ex.getMessage());
            }
        }

        log.debug("Movimientos (desktop) extraídos: {}", movimientos.size());
        return movimientos;
    }

    /**
     * Normaliza un texto
     * @param cell elemento de pagina
     * @return {@link String}
     */
    private String normalizeText(Locator cell) {
        String text = cell.textContent();
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Formatea descripcion del movimiento
     * @param movimiento descripcion del movimiento
     * @param detalle detalle del movimiento
     * @return {@link String}
     */
    private String buildDescripcion(String movimiento, String detalle) {
        if (movimiento.isEmpty()) {
            return detalle;
        }
        if (detalle.isEmpty() || detalle.equals("-")) {
            return movimiento;
        }
        return movimiento + " - " + detalle;
    }

    /**
     * Extrae el dato de monto de la columna del elemento de la pagina
     * @param cell elemento de pagina
     * @return {@link double}
     */
    private double parseAmountColumn(Locator cell) {
        try {
            Locator amountSpan = cell.locator("span.label-balance__amount").first();
            if (amountSpan.count() > 0) {
                String amountText = amountSpan.textContent() == null ? "" : amountSpan.textContent().trim();
                return MetodsGeneric.parseSaldo(amountText);
            }
        } catch (Exception e) {
            log.error("Error parsing amount column: {}", e.getMessage());
        }
        String raw = cell.textContent() == null ? "" : cell.textContent().trim();
        return MetodsGeneric.parseSaldo(raw);
    }

    /**
     * Detecta si el boton esta activado o desactivado
     * @param button Elemento de la pagina a evaluar
     * @return {@link boolean}
     */
    private boolean isButtonDisabled(Locator button) {
        String disabledAttr = button.getAttribute("disabled");
        String ariaDisabled = button.getAttribute("aria-disabled");
        return disabledAttr != null || "true".equalsIgnoreCase(ariaDisabled);
    }

    /**
     * Inserta la fecha en los campos de busqueda
     * @param input elemento de la pagina donde se ingresara el valor
     * @param value valor
     */
    private void setInputValue(Locator input, String value) {
        input.evaluate(
                "(el, v) => { el.value = v; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); el.dispatchEvent(new Event('blur', { bubbles: true })); }",
                value
        );
    }
}
