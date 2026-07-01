package com.raissa.rpa.service.impl.ibk;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.config.SvgDigitClassifier;
import com.raissa.rpa.exception.IbkException;
import com.raissa.rpa.service.commons.NavigatorService;
import com.raissa.rpa.service.ibk.IBKEmpresaService;
import com.raissa.rpa.service.ibk.IbkMenuService;
import com.raissa.rpa.service.impl.commons.BaseBankService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class IBKEmpresaServiceImpl extends BaseBankService implements IBKEmpresaService {
    @Value("${banking.ibk.url}")
    private String ibkUrl;

    private final IbkMenuService ibkMenuService;

    public IBKEmpresaServiceImpl(NavigatorService navigatorService, IbkMenuService ibkMenuService) {
        super(navigatorService);
        this.ibkMenuService = ibkMenuService;
    }

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login IBK");

        NavigatorSession sessionNavegacion = null;
        boolean success = false;
        Map<String, Object> result;

        try {
            sessionNavegacion = navigatorService.iniciarNavegador(transactionId, "INTERBANK");
            Page page = sessionNavegacion.page();

            page.navigate(ibkUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(60_000));

            MetodsGeneric.randomWaitPage(page,800, 1_000);

            log.info("Navegando a: {}", ibkUrl);

            // 1. ✅ Seleccionar tipo de documento (toggle + combo)
            String docSelector = credentials.get("codigoEmpresa");
            selectLoginDocumentType(page, docSelector);

            MetodsGeneric.randomWaitPage(page,1_000, 2_000);

            String usuario = credentials.get("codigoUsuario");

            if (usuario == null || usuario.isBlank()) {
                throw new IbkException("El usuario de acceso no puede estar vacío",
                        "IBK_EMPTY_USER",
                        "Ingresa un usuario válido para continuar");
            }
            if (docSelector.equals("TIE")) {
                if (usuario.length() != 16) {
                    throw new IbkException("El usuario de acceso no tiene exactamente 16 caracteres",
                            "IBK_USER_TOO_LONG",
                            "Cambia la longitud del usuario e inténtalo nuevamente");
                }
            } else {
                if (usuario.length() > 50) {
                    throw new IbkException("El usuario de acceso excede el máximo permitido (50 caracteres)",
                            "IBK_USER_TOO_LONG",
                            "Reduce la longitud del usuario e inténtalo nuevamente");
                }
            }

            // 2. ✅ Ingresar código de usuario
            enterUserCode(page, docSelector, usuario);

            // 2. ✅ INGRESAR CLAVE
            String claveAcceso = credentials.get("claveAcceso");
            if (claveAcceso == null || claveAcceso.isBlank()) {
                throw new IbkException("La clave de acceso no puede estar vacía",
                        "IBK_EMPTY_PASSWORD",
                        "Ingresa una clave válida para continuar");
            }
            if (docSelector.equals("TIE")) {
                if (claveAcceso.length() != 4) {
                    throw new IbkException("La clave de acceso no tiene exactamente 4 caracteres",
                            "IBK_PASSWORD_TOO_LONG",
                            "Cambia la longitud de la clave e inténtalo nuevamente");
                }
            } else {
                if (claveAcceso.length() > 50) {
                    throw new IbkException("La clave de acceso excede el máximo permitido (50 caracteres)",
                            "IBK_PASSWORD_TOO_LONG",
                            "Reduce la longitud de la clave e inténtalo nuevamente");
                }
            }

            enterPassword(page, docSelector, claveAcceso);
            MetodsGeneric.randomWaitPage(page, 300, 500);

            // 3. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(page);

            // 6. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = ibkMenuService.verifyLoginSuccess(page);

            if (!loginSuccess) {
                throw new IbkException("Error en el login después de enviar formulario",
                        "IBK_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login exitoso");
            }

            // 7. ✅ ÉXITO - Almacenar driver y retornar resultado
            cacheSession(transactionId, sessionNavegacion);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login IBK exitoso", true);
            success = true;

            log.info("Login IBK completado exitosamente [tx={}]", transactionId);

            return result;
        } catch (Exception e) {
            log.error("Error en login IBK [tx={}]: {}", transactionId, e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(
                    transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "IBK_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());

            return errorResult;
        } finally {
            if (sessionNavegacion != null && !success) {
                releaseSessionOnError(transactionId, "INTERBANK", sessionNavegacion);
            }
        }
    }

    public Map<String, Object> obtenerSaldo(String transactionId) {
        Map<String, Object> result;
        log.info("Obteniendo saldo IBK, transactionId: {}", transactionId);

        try {
            NavigatorSession session = getSession(transactionId);
            Page page = session.page();

            if (!ibkMenuService.closeCampaignPopupIfPresent(page)) {
                log.info("No hay popup que cerrar");
            }

            if (!ibkMenuService.verifyLoginSuccess(page)) {
                throw new IbkException("No se pudo navegar a cuentas",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 1. ✅ Hacer clic en "consultas"
            if (!ibkMenuService.clickConsultas(page)) {
                throw new IbkException("No se pudo navegar a saldos",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de saldos");
            }

            // 2. ✅ Extraer datos de las cuentas
            List<Map<String, Object>> accounts = ibkMenuService.extractAccountsData(page);

            // 3. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put("data", accounts);
            result.put("count", accounts.size());

            return result;
        } catch (Exception e) {
            log.error("Error obteniendo saldo IBK: {}", e.getMessage());

            // ✅ Si hay error, liberar la sesión
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session != null) {
                releaseSessionOnError(transactionId, "INTERBANK", session);
            }

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result = null;
        log.info("Obteniendo movimientos IBK, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            NavigatorSession session = getSession(transactionId);
            Page page = session.page();

            // 1. ✅ Navegar a la opción "Movimientos" del menú lateral
            if (!ibkMenuService.clickMovimientos(page)) {
                throw new IbkException("No se pudo navegar a movimietnos",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de movimientos");
            }

            // 3. ✅ Seleccionar la cuenta específica
            if (!ibkMenuService.selectCuenta(page, numeroCuenta)) {
                throw new IbkException("No se pudo seleccionar la cuenta",
                        "IBK_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            if (!ibkMenuService.setDateRange(page, fechaInicio, fechaFin)) {
                throw new IbkException("No se pudo configurar el rango de fechas",
                        "IBK_DATE_RANGE_ERROR",
                        "Error al establecer fechas: " + fechaInicio + " - " + fechaFin);
            }

            // 5. ✅ Aplicar filtros y esperar resultados
            ibkMenuService.applyFilters(page);
            ibkMenuService.waitForMovimientosToLoad(page);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = ibkMenuService.extractMovimientosData(page);

            // 7. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put("data", movimientos);
            result.put("count", movimientos.size());
            result.put("cuenta", numeroCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo movimientos IBK: {}", e.getMessage());

            // ✅ Si hay error, liberar la sesión
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session != null) {
                releaseSessionOnError(transactionId, "INTERBANK", session);
            }

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> logout(String transactionId) {
        NavigatorSession session = getSession(transactionId);
        Page page = session.page();

        try {
            log.info("Iniciando proceso de logout del ibk para transactionId: {}", transactionId);

            // 1. ✅ ABRIR EL DROPDOWN DE PERFIL (si no está visible)
            ibkMenuService.openProfileDropdown(page);

            // 2. ✅ HACER CLICK EN "CERRAR SESIÓN"
            ibkMenuService.clickLogoutButton(page);

            // 3. ✅ MODAL CUANDO SE MUESTRA
            ibkMenuService.handleNpsSurvey(page);

            // 4. ✅ VERIFICAR QUE EL LOGOUT FUE EXITOSO
            boolean logoutSuccess = ibkMenuService.verifyLogoutSuccess(page);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            }

            // ✅ Liberar sesión en el pool
            releaseSessionOnLogout(transactionId, "INTERBANK", session);

            log.debug("Driver cerrado exitosamente");

            return ResponseGeneric.buildSuccessResponse(
                    transactionId, "Sesión cerrada exitosamente", true);
        } catch (Exception e) {
            log.error("Error durante logout [tx={}]: {}", transactionId, e.getMessage());

            // ✅ Liberar sesión incluso en error
            releaseSessionOnError(transactionId, "INTERBANK", session);

            throw new IbkException("Error en logout", "IBK_LOGOUT_ERROR", e.getMessage());
        }
    }

    /**
     * Selecciona metodo de ingreso de credenciales
     *
     * @param page manejador de pagina
     * @param selectorValue Valor para seleccioanr metodo de ingreso
     */
    private void selectLoginDocumentType(Page page, String selectorValue) {
        if (selectorValue == null || selectorValue.isBlank()) {
            return;
        }

        String normalized = selectorValue.trim().toUpperCase(Locale.ROOT);
        int timeoutMs = 8_000;

        switch (normalized) {
            case "TIE":
                // Cambiar a la pestaña TIE
                Locator loc = MetodsGeneric.waitForVisible(page, "//mat-button-toggle[.//span[contains(normalize-space(.),'TIE')]]//button", timeoutMs);
                MetodsGeneric.clickWithFallback(page, loc, timeoutMs);
                break;

            case "DNI":
                ensureDocIdentidadToggle(page, timeoutMs);
                break;

            case "CE":
                ensureDocIdentidadToggle(page, timeoutMs);
                selectMatOption(page, "CE");
                break;

            case "PAS":
                ensureDocIdentidadToggle(page, timeoutMs);
                selectMatOption(page, "Pasaporte");
                break;

            default:
                ensureDocIdentidadToggle(page, timeoutMs);
                break;
        }
    }

    /**
     * Hace clic en la pestaña de documento
     * @param page manejador de pagina
     * @param timeoutMs tiempo de espera
     */
    private void ensureDocIdentidadToggle(Page page, int timeoutMs) {
        Locator docToggle = MetodsGeneric.waitForVisible(page, "//mat-button-toggle[.//span[contains(normalize-space(.),'Doc. Identidad')]]//button", timeoutMs);
        String ariaPressed = docToggle.getAttribute("aria-pressed");
        boolean isPressed = Boolean.parseBoolean(ariaPressed);
        if (!isPressed) {
            MetodsGeneric.clickWithFallback(page, docToggle, timeoutMs);
        }
    }

    /**
     *  Selecciona la opcion de documento correspondiente
     *
     * @param page  manejador de pagina
     * @param optionText texto normalizado para obtener valor de seleccion
     */
    private void selectMatOption(Page page, String optionText) {
        int timeoutMs = 8_000;

        // 1) Abrir el mat-select
        Locator matSelect = MetodsGeneric.waitForVisible(page, "mat-select[data-test='cmbTypeDoc']", timeoutMs);
        MetodsGeneric.clickWithFallback(page, matSelect, timeoutMs);

        // 2) Elegir la opción por texto
        Locator option = MetodsGeneric.waitForVisible(page, String.format(
                "//mat-option//span[contains(normalize-space(.),'%s')]",
                optionText), timeoutMs);
        MetodsGeneric.clickWithFallback(page, option, timeoutMs);

        // 3) Esperar a que cierre el panel del overlay
        Locator panel = page.locator(".cdk-overlay-pane .mat-mdc-select-panel");
        panel.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(timeoutMs));
    }

    /**
     * Ingresa el código del usuario en este caso número de tarjeta
     *
     * @param page manejador de pagina
     * @param docSelector para saber en qué objeto ingresará valores de logueo
     * @param userValue código de usuario o número de tarjeta
     */
    private void enterUserCode(Page page, String docSelector, String userValue) {
        log.info("Ingresando tarjeta/código de usuario...");

        try {
            if (userValue == null || userValue.isBlank()) {
                throw new IbkException("El identificador de usuario no puede estar vacío",
                        "IBK_EMPTY_USER",
                        "Ingresa el usuario o número de documento para continuar");
            }

            int timeoutMs = 8_000;
            String normalizedSelector = docSelector == null ? "" : docSelector.trim().toUpperCase(Locale.ROOT);

            Locator input;
            if ("TIE".equals(normalizedSelector)) {
                input = MetodsGeneric.waitForVisible(page, "input#txtTie[data-test='txtTie']", timeoutMs);
            } else {
                input = MetodsGeneric.waitForVisible(page, "input#login-doc[data-test='txtNumDoc']", timeoutMs);
            }

            input.fill("", new Locator.FillOptions().setTimeout(timeoutMs));

            MetodsGeneric.humanTypeText(input, userValue.trim(), 120, 220);
        } catch (TimeoutError e) {
            throw IbkException.elementNotFound("input login", "input user");
        } catch (Exception e) {
            throw new IbkException("Error al aplicar tabs de navegación",
                    "IBK_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el password en la caja de contraseña
     *
     * @param page manejador de página
     * @param password contraseña a ingresar
     */
    private void enterPassword(Page page, String docSelector, String password) {
        log.info("Ingresando clave de {} dígitos...", password.length());

        try {
            int timeoutMs = 8_000;
            String normalizedSelector = docSelector == null ? "" : docSelector.trim().toUpperCase(Locale.ROOT);

        if ("TIE".equals(normalizedSelector)) {
            Locator tiePasswordInput = MetodsGeneric.waitForVisible(page, "input#passwordTie[data-test='txtPassword']", timeoutMs);
            MetodsGeneric.clickWithFallback(page, tiePasswordInput, timeoutMs);
            MetodsGeneric.randomWaitPage(page, 200, 350);

            Map<String, Locator> keyMap = mapVirtualKeyboard(page);

            for (char ch : password.toCharArray()) {
                String key = String.valueOf(ch);
                Locator keyButton = keyMap.get(key.toUpperCase(Locale.ROOT));
                if (keyButton == null) {
                    throw new IbkException("Carácter no disponible en teclado virtual: " + ch,
                            "IBK_KEY_NOT_FOUND",
                            "El teclado no contiene la tecla " + ch);
                }
                clickVirtualKey(page, keyButton, key);
            }

            log.info("Clave TIE ingresada exitosamente mediante teclado virtual");

        } else {
            Locator docPasswordInput = MetodsGeneric.waitForVisible(page, "input#passwordDoc[data-test='txtPassword']", timeoutMs);
            String readonly = docPasswordInput.getAttribute("readonly");

            if ("true".equalsIgnoreCase(readonly)) {
                page.evaluate("el => el.removeAttribute('readonly')", docPasswordInput);
            }

            MetodsGeneric.clickWithFallback(page, docPasswordInput, timeoutMs);
            docPasswordInput.fill("", new Locator.FillOptions().setTimeout(timeoutMs));
            MetodsGeneric.humanTypeText(docPasswordInput, password.trim(), 120, 220);

            log.info("Clave ingresada exitosamente en campo de texto");
        }
        } catch (IbkException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado ingresando clave: {}", e.getMessage());
            throw new IbkException("Error inesperado ingresando clave",
                    "IBK_PASSWORD_ERROR",
                    "Error al ingresar la clave en el teclado virtual");
        }
    }

    /**
     * Mapea teclado virtual
     *
     * @param page manejador de página
     * @return {@link Map}
     */
    private Map<String, Locator> mapVirtualKeyboard(Page page) {
        log.info("Mapeando teclado virtual del IBK...");

        Map<String, Locator> keyMap = new HashMap<>();

        try {
            int timeoutMs = 6_000;

            Locator keyboard = MetodsGeneric.waitForVisible(page, "ibk-keyboard[data-test='tblKeyboard']", timeoutMs);

            Locator keys = keyboard.locator(
                    "ibk-button.ibk-keyboard__btn:not(.ibk-keyboard__btn--reset):not(.ibk-keyboard__btn--delete) > button"
            );

            int count = keys.count();
            for (int i = 0; i < count; i++) {
                Locator button = keys.nth(i);

                String dataTest = button.getAttribute("data-test");
                if ("btnLoad".equalsIgnoreCase(dataTest)) {
                    continue;
                }

                String rawLabel = processKeyboardKey(page, button);
                if (rawLabel == null || rawLabel.isBlank()) {
                    log.debug("Botón omitido por carecer de etiqueta resoluble: {}", button);
                    continue;
                }

                String normalizedKey = rawLabel.trim().toUpperCase(Locale.ROOT);
                Locator previous = keyMap.putIfAbsent(normalizedKey, button);
                if (previous != null) {
                    log.warn("Clave duplicada en teclado virtual: {}", normalizedKey);
                }
            }

            if (keyMap.isEmpty()) {
                throw new IllegalStateException("No se pudo mapear ninguna tecla del teclado virtual.");
            }

            return keyMap;

        } catch (TimeoutError e) {
            log.error("Timeout buscando teclado virtual: {}", e.getMessage(), e);
            throw new IbkException("Timeout teclado virtual",
                    "IBK_KEYBOARD_TIMEOUT",
                    "No se pudo encontrar el teclado virtual en el tiempo esperado");

        } catch (Exception e) {
            log.error("Error mapeando teclado virtual: {}", e.getMessage(), e);
            throw new IbkException("Error mapeando teclado virtual",
                    "IBK_KEYBOARD_MAPPING_ERROR",
                    "Error al procesar el teclado del banco: " + e.getMessage());
        }
    }

    private String processKeyboardKey(Page page, Locator button) {
        try {
            String label = firstNonBlankAttribute(button, "aria-label", "data-key", "value");
            if (!label.isBlank()) {
                return label.trim();
            }

            Optional<String> svgValue = resolveKeyFromSvg(button);
            if (svgValue.isPresent()) {
                return svgValue.get();
            }

            String fallback = page.evaluate(
                    "el => (el.textContent && el.textContent.trim()) || ''",
                    button
            ).toString();
            return Optional.ofNullable(fallback).orElse("");

        } catch (Exception e) {
            log.warn("Error procesando tecla: {}", e.getMessage(), e);
            return "";
        }
    }

    private Optional<String> resolveKeyFromSvg(Locator button) {
        Locator paths = button.locator("svg path[d]");
        int pathCount = paths.count();

        for (int i = 0; i < pathCount; i++) {
            Locator path = paths.nth(i);
            String dAttribute = path.getAttribute("d");
            if (dAttribute == null || dAttribute.isBlank()) {
                continue;
            }
            try {
                Optional<String> digit = SvgDigitClassifier.classify(dAttribute);
                if (digit.isPresent()) {
                    return digit;
                }
                log.debug("No se encontró coincidencia para path: {}", dAttribute);
            } catch (ParseException ex) {
                log.warn("Path SVG inválido ({}) → {}", dAttribute, ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private String firstNonBlankAttribute(Locator element, String... attributes) {
        for (String attribute : attributes) {
            String value = element.getAttribute(attribute);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * Hacer clic en cada dígito de la contraseña
     *
     * @param page manejador de página
     * @param keyButton indice de tecla
     * @param keyLabel valor del boton
     */
    private void clickVirtualKey(Page page, Locator keyButton, String keyLabel) {
        try {
            keyButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(4_000));
            MetodsGeneric.clickWithFallback(page, keyButton, 4_000);

            log.debug("Click en tecla '{}'", keyLabel);

            MetodsGeneric.randomWaitPage(page, 300, 500);

        } catch (Exception e) {
            log.error("Error haciendo click en tecla '{}': {}", keyLabel, e.getMessage());
            throw new IbkException("Error al hacer click en tecla virtual",
                    "IBK_KEY_CLICK_ERROR",
                    "Error al interactuar con el teclado del banco");
        }
    }

    /**
     * Hace clic en el boton de ingresar
     *
     * @param page manejador de página
     */
    private void clickLoginButton(Page page) {
        log.info("Haciendo click en botón de login...");

        try {
            int timeoutMs = 8_000;
            Locator loginButton = MetodsGeneric.waitForVisible(page, "//button//span[normalize-space()='Iniciar sesión']", timeoutMs);
            MetodsGeneric.clickWithFallback(page, loginButton, timeoutMs);
            log.info("Click en botón de login realizado");

            MetodsGeneric.randomWaitPage(page, 2500, 3500);

        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new IbkException("Error al hacer click en botón de login",
                    "IBK_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }
}
