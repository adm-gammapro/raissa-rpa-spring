package com.raissa.rpa.service.impl.bcp;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.bcp.BCPEmpresaService;
import com.raissa.rpa.service.bcp.BcpMenuService;
import com.raissa.rpa.service.commons.NavigatorService;
import com.raissa.rpa.service.impl.commons.BaseBankService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Normal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BCPEmpresaServiceImpl extends BaseBankService implements BCPEmpresaService {
    @Value("${banking.bcp.url}")
    private String bcpUrl;

    @Value("${2captcha.api.key}")
    private String apiKey;

    @Value("${2captcha.timeout}")
    private String timeoutStr;

    @Value("${2captcha.polling.interval}")
    private String pollingStr;

    private final BcpMenuService bcpMenuService;

    // Constructor explícito para inyectar NavigatorService
    public BCPEmpresaServiceImpl(NavigatorService navigatorService, BcpMenuService bcpMenuService) {
        super(navigatorService);
        this.bcpMenuService = bcpMenuService;
    }

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login BCP");

        NavigatorSession sessionNavegacion = null;
        boolean success = false;
        Map<String, Object> result;

        try {
            sessionNavegacion = navigatorService.iniciarNavegador(transactionId, "BCP");
            Page page = sessionNavegacion.page();

            page.navigate(bcpUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(60_000));

            MetodsGeneric.randomWaitPage(page,800, 1000);

            log.info("Navegando a: {}", bcpUrl);

            // 1. ✅ Ingresar código de usuario
            enterUserCode(page, credentials.get("codigoUsuario"));

            // 2. ✅ INGRESAR CLAVE CON TECLADO VIRTUAL
            String claveAcceso = credentials.get("claveAcceso");
            if (claveAcceso == null || claveAcceso.length() != 6) {
                throw new BcpException("Clave de acceso debe tener 6 dígitos",
                        "BCP_INVALID_PASSWORD_LENGTH",
                        "La clave debe tener exactamente 6 dígitos");
            }

            enterPassword(page, claveAcceso);
            MetodsGeneric.randomWaitPage(page,300, 500);

            // 3. ✅ MANEJAR CAPTCHA
            handleCaptcha(page);

            // 4. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(page);

            // 5. Verificar si el captcha fue aceptado
            if (!isCaptchaAccepted(page)) {
                throw new BcpException("Solución de captcha rechazada",
                        "BCP_CAPTCHA_REJECTED",
                        "La solución del captcha no fue aceptada");
            }

            // 6. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = bcpMenuService.verifyLoginSuccess(page);

            if (!loginSuccess) {
                throw new BcpException("Error en el login después de enviar formulario",
                        "BCP_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login exitoso");
            }

            cacheSession(transactionId, sessionNavegacion);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login BCP exitoso", true);

            log.info("Login BCP completado exitosamente");

            success = true;

            return result;
        } catch (Exception e) {
            log.error("Error genérico en login BCP: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "BCP_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());

            return errorResult;
        } finally {
            if (sessionNavegacion != null && !success) {
                releaseSessionOnError(transactionId, "BCP", sessionNavegacion);
            }
        }
    }

    public Map<String, Object> obtenerSaldo(String transactionId) {
        log.info("Obteniendo saldo BCP, transactionId: {}", transactionId);

        try {
            NavigatorSession session = getSession(transactionId);
            Page page = session.page();

            // 0. ✅ Manejar el modal móvil si está abierto
            bcpMenuService.handleMobileModal(page);

            if (!bcpMenuService.isOnAccountsPage(page)) {
                throw new BcpException("No se pudo navegar a cuentas",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 1. ✅ Hacer clic en el tab "Cuentas"
            bcpMenuService.clickAccountsTab(page);

            // 2. ✅ Esperar a que carguen los datos
            bcpMenuService.waitForAccountsToLoad(page);

            // 3. ✅ Extraer datos de las cuentas
            List<Map<String, Object>> accounts = bcpMenuService.extractAccountsData(page);

            // 4. ✅ Retornar resultados
            Map<String, Object> result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, accounts);
            result.put(Constantes.KEY_COUNT, accounts.size());

            return result;
        } catch (Exception e) {
            log.error("Error obteniendo saldo BCP: {}", e.getMessage());

            // ✅ Si hay error, liberar la sesión
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session != null) {
                releaseSessionOnError(transactionId, "BCP", session);
            }

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            NavigatorSession session = getSession(transactionId);
            Page page = session.page();

            // 1. ✅ Navegar a la opción "Resumen" del menú lateral
            bcpMenuService.navigateToResumen(page);

            // 2. ✅ Esperar a que cargue la página de resumen
            bcpMenuService.waitForResumenAccountsToLoad(page);

            // 3. ✅ Seleccionar la cuenta específica
            if (!bcpMenuService.selectCuenta(page, numeroCuenta)) {
                throw new BcpException("No se pudo seleccionar la cuenta",
                        "BCP_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            bcpMenuService.setDateRange(page, fechaInicio, fechaFin);

            // 5. ✅ Aplicar filtros y esperar resultados
            bcpMenuService.applyFilters(page);
            bcpMenuService.waitForMovimientosToLoad(page);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bcpMenuService.extractMovimientosData(page);

            // 7. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, movimientos);
            result.put(Constantes.KEY_COUNT, movimientos.size());
            result.put(Constantes.KEY_CUENTA, numeroCuenta);
            result.put(Constantes.KEY_FECHA_INICIO, fechaInicio);
            result.put(Constantes.KEY_FECHA_FIN, fechaFin);

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo movimientos BCP: {}", e.getMessage());

            // ✅ Si hay error, liberar la sesión
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session != null) {
                releaseSessionOnError(transactionId, "BCP", session);
            }

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientosHistorico(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos históricos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            NavigatorSession session = getSession(transactionId);
            Page page = session.page();

            // 1. ✅ Navegar a la opción "Resumen" del menú lateral
            bcpMenuService.navigateToResumen(page);

            // 2. ✅ Esperar a que cargue la página de resumen
            bcpMenuService.waitForResumenAccountsToLoad(page);

            // 3. ✅ Seleccionar la cuenta específica
            if (!bcpMenuService.selectCuentaHistorico(page, numeroCuenta)) {
                throw new BcpException("No se pudo seleccionar la cuenta",
                        "BCP_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            bcpMenuService.setDateRange(page, fechaInicio, fechaFin);

            // 5. ✅ Aplicar filtros y esperar resultados
            bcpMenuService.applyFiltersHistorico(page);
            bcpMenuService.waitForMovimientosToLoad(page);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bcpMenuService.extractMovimientosData(page);

            // 7. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put("data", movimientos);
            result.put("count", movimientos.size());
            result.put("cuenta", numeroCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo movimientos BCP: {}", e.getMessage());

            // ✅ Si hay error, liberar la sesión
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session != null) {
                releaseSessionOnError(transactionId, "BCP", session);
            }

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> logout(String transactionId) {
        NavigatorSession session = getSession(transactionId);
        Page page = session.page();

        try {
            log.info("Iniciando proceso de logout para transactionId: {}", transactionId);

            // 1. ✅ ABRIR EL DROPDOWN DE PERFIL (si no está visible)
            bcpMenuService.openProfileDropdown(page);

            // 2. ✅ HACER CLICK EN "CERRAR SESIÓN"
            bcpMenuService.clickLogoutButton(page);

            // 3. ✅ MANEJAR POSIBLE ENCUESTA NPS (si aparece)
            bcpMenuService.handleNpsSurvey(page);

            // 4. ✅ VERIFICAR QUE EL LOGOUT FUE EXITOSO
            boolean logoutSuccess = bcpMenuService.verifyLogoutSuccess(page);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            }

            // ✅ Liberar sesión en el pool
            releaseSessionOnLogout(transactionId, "BCP", session);

            return ResponseGeneric.buildSuccessResponse(transactionId, "Sesión cerrada exitosamente", true);
        } catch (Exception e) {
            log.error("Error durante logout [tx={}]: {}", transactionId, e.getMessage());

            // ✅ Liberar sesión incluso en error
            releaseSessionOnError(transactionId, "BCP", session);

            throw new BcpException("Error en logout", "BCP_LOGOUT_ERROR", e.getMessage());
        }
    }

    /**
     * Ingresa el código del usuario en este caso número de tarjeta
     *
     * @param page manejador de pagina
     * @param userCode código de usuario o número de tarjeta
     */
    private void enterUserCode(Page page, String userCode) {
        log.info("Ingresando tarjeta/código de usuario...");

        try {
            Locator inputLogin = MetodsGeneric.waitForVisible(page, "input[name='ciam-input-card']", 12_000);

            inputLogin.fill("");
            MetodsGeneric.humanTypeText(inputLogin, userCode, 120, 220);

            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page,300, 500);
            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page,300, 500);

            log.info("Usuario ingresado y tabs aplicados");
        } catch (TimeoutError e) {
            throw BcpException.elementNotFound("input login", "input user");
        } catch (Exception e) {
            throw new BcpException("Error al aplicar tabs de navegación",
                    "BCP_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el password en la caja de contraseña
     *
     * @param page manejador de página
     * @param password contraseña a ingresar
     */
    private void enterPassword(Page page, String password) {
        log.info("Ingresando clave de {} dígitos...", password.length());

        try {
            Map<String, Integer> keyMap = mapVirtualKeyboard(page);

            if (keyMap.isEmpty()) {
                throw new BcpException("No se encontraron teclas en el teclado virtual",
                        "BCP_KEYBOARD_EMPTY",
                        "El teclado virtual no está disponible");
            }

            for (char digit : password.toCharArray()) {
                String digitStr = String.valueOf(digit);
                if (!keyMap.containsKey(digitStr)) {
                    throw new BcpException("Dígito no encontrado en teclado: " + digit,
                            "BCP_DIGIT_NOT_FOUND",
                            "El teclado no tiene el dígito " + digit);
                }
            }

            for (char digit : password.toCharArray()) {
                String d = String.valueOf(digit);
                Integer keyIndex = keyMap.get(d);
                log.info("Ingresando dígito: {} -> tecla índice: {}", digit, keyIndex);
                clickVirtualKey(page, keyIndex);
            }

            log.info("Clave ingresada exitosamente");
        } catch (BcpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado ingresando clave: {}", e.getMessage());
            throw new BcpException("Error inesperado ingresando clave",
                    "BCP_PASSWORD_ERROR",
                    "Error al ingresar la clave en el teclado virtual");
        }
    }

    /**
     * Mapea teclado virtual
     *
     * @param page manejador de página
     * @return {@link Map}
     */
    private Map<String, Integer> mapVirtualKeyboard(Page page) {
        log.info("Mapeando teclado virtual del BCP...");

        Map<String, Integer> keyMap = new LinkedHashMap<>();

        try {
            Locator passwordBox = MetodsGeneric.waitForVisible(page, "bcp-input-password .input-password", 5_000);
            passwordBox.click();

            Locator keyboardKeys = page.locator("bcp-input-password bcp-keyboard-key[index]");
            keyboardKeys.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5_000));

            long total = keyboardKeys.count();
            log.info("Teclas encontradas: {}", total);

            if (total == 0) {
                throw new BcpException("Teclado virtual no encontrado",
                        "BCP_KEYBOARD_NOT_FOUND",
                        "No se encontraron teclas del teclado virtual");
            }

            int processed = 0;
            for (int i = 0; i < total; i++) {
                Locator key = keyboardKeys.nth(i);
                String idx = key.getAttribute("index");
                String label = key.locator(".digit-number").textContent().trim();
                if (!label.isEmpty() && idx != null) {
                    keyMap.put(label, Integer.parseInt(idx));
                    processed++;
                }
            }

            log.info("Teclas procesadas exitosamente: {}/{}", processed, total);
            log.debug("Teclado virtual mapeado: {}", keyMap);

            validateKeyMapping(keyMap);

            return keyMap;

        } catch (TimeoutError e) {
            log.error("Timeout buscando teclado virtual: {}", e.getMessage());
            throw new BcpException("Timeout teclado virtual",
                    "BCP_KEYBOARD_TIMEOUT",
                    "No se pudo encontrar el teclado virtual en el tiempo esperado");

        } catch (Exception e) {
            log.error("Error mapeando teclado virtual: {}", e.getMessage());
            throw new BcpException("Error mapeando teclado virtual",
                    "BCP_KEYBOARD_MAPPING_ERROR",
                    "Error al procesar el teclado del banco: " + e.getMessage());
        }
    }

    /**
     * Validar que el mapeo del teclado sea completo
     *
     * @param keyMap mapa de teclas
     */
    private void validateKeyMapping(Map<String, Integer> keyMap) {
        if (keyMap.size() < 10) {
            log.warn("Mapeo incompleto. Solo {} teclas de 10 posibles", keyMap.size());
        }

        Set<String> expectedDigits = Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        Set<String> missingDigits = expectedDigits.stream()
                .filter(digit -> !keyMap.containsKey(digit))
                .collect(Collectors.toSet());

        if (!missingDigits.isEmpty()) {
            log.warn("Dígitos faltantes en el teclado: {}", missingDigits);
        }
    }

    /**
     * Hacer clic en cada dígito de la contraseña
     *
     * @param page manejador de página
     * @param keyIndex indice de tecla
     */
    private void clickVirtualKey(Page page, int keyIndex) {
        try {
            String selector = String.format("bcp-keyboard-key[index='%d']", keyIndex);
            Locator key = page.locator(selector);

            key.click(new Locator.ClickOptions().setTimeout(4_000));
            MetodsGeneric.randomWaitPage(page,300, 500);
        } catch (Exception e) {
            log.error("Error haciendo click en tecla {}: {}", keyIndex, e.getMessage());
            throw new BcpException("Error al hacer click en tecla virtual",
                    "BCP_KEY_CLICK_ERROR",
                    "Error al interactuar con el teclado del banco");
        }
    }

    /**
     * Interpreta imagen captcha
     *
     * @param page manejador de página
     */
    private void handleCaptcha(Page page) {
        log.info("Manejando captcha del BCP...");

        try {
            // 1. Obtener imagen del captcha (con reintentos)
            String captchaImageBase64 = extractCaptchaBase64(page);

            // 2. Resolver el captcha
            String captchaSolution = resolveCaptcha(captchaImageBase64);

            // 3. Ingresar la solución
            enterCaptchaSolution(page, captchaSolution);

            log.info("Captcha manejado exitosamente");
        } catch (Exception e) {
            log.error("Error manejando captcha: {}", e.getMessage());
            throw new BcpException("Error inesperado manejando captcha",
                    "BCP_CAPTCHA_HANDLING_ERROR",
                    "Error durante el proceso de captcha: " + e.getMessage());
        }
    }

    /**
     * Extrae la imagen base64 del elemento captcha BCP
     *
     * @param page WebDriver
     * @return String base64 de la imagen
     */
    private String extractCaptchaBase64(Page page) {
        log.info("Extrayendo captcha BCP...");

        try {
            String css = "bcp-captcha bcp-img img[src^='data:image']";
            Locator captchaImg = MetodsGeneric.waitForVisible(page, css, 8_000);
            String src = captchaImg.getAttribute("src");
            log.info("Captcha encontrado, SRC length: {}", src != null ? src.length() : 0);
            return extractBase64FromSrc(src);

        } catch (Exception e) {
            log.error("Error encontrando captcha: {}", e.getMessage());
            throw new BcpException("Captcha no encontrado",
                    "BCP_CAPTCHA_NOT_FOUND",
                    "No se pudo localizar el elemento del captcha: " + e.getMessage());
        }
    }

    /**
     * Extrae la parte base64 de un data URI de imagen
     *
     * @param src El data URI completo (ej: "data:image/jpeg;base64,/9j/4AAQ...")
     * @return Solo la parte base64 (ej: "/9j/4AAQ...")
     * @throws BcpException Si el formato es inválido
     */
    private String extractBase64FromSrc(String src) {
        log.debug("Extrayendo base64 de SRC: {}", src != null ? src.substring(0, Math.min(100, src.length())) + "..." : "null");

        if (src == null) {
            throw new BcpException("SRC nulo",
                    "BCP_CAPTCHA_SRC_NULL",
                    "El atributo src del captcha es nulo");
        }

        if (!src.startsWith("data:image")) {
            throw new BcpException("Formato no soportado",
                    "BCP_CAPTCHA_FORMAT_ERROR",
                    "El src no es un data URI de imagen: " + src.substring(0, Math.min(100, src.length())));
        }

        if (!src.contains(",")) {
            throw new BcpException("Formato inválido",
                    "BCP_CAPTCHA_INVALID_FORMAT",
                    "El data URI no contiene separador de coma: " + src.substring(0, Math.min(100, src.length())));
        }

        String[] parts = src.split(",", 2);
        if (parts.length < 2) {
            throw new BcpException("Base64 no encontrado",
                    "BCP_CAPTCHA_NO_BASE64",
                    "No se pudo extraer base64 del src");
        }

        String base64 = parts[1].trim();

        if (base64.isEmpty()) {
            throw new BcpException("Base64 vacío",
                    "BCP_CAPTCHA_EMPTY_BASE64",
                    "La parte base64 está vacía");
        }

        // 6. Validar formato base64
        validateBase64Format(base64);

        log.info("Base64 extraído exitosamente ({} caracteres)", base64.length());
        return base64;
    }

    /**
     * Valida el formato del string base64
     *
     * @param base64 cadena en base64
     */
    private void validateBase64Format(String base64) {
        if (base64.length() < 50) {
            throw new BcpException("Base64 demasiado corto",
                    "BCP_CAPTCHA_SHORT_BASE64",
                    "El base64 es demasiado corto: " + base64.length() + " caracteres");
        }

        if (!base64.matches("[a-zA-Z0-9+/=]+")) {
            log.warn("Base64 contiene caracteres inusuales, verificando...");

            String cleaned = base64.replaceAll("[^a-zA-Z0-9+/=]", "");
            if (cleaned.length() < base64.length() * 0.9) {
                throw new BcpException("Base64 inválido",
                        "BCP_CAPTCHA_INVALID_BASE64",
                        "El base64 contiene demasiados caracteres inválidos");
            }
        }

        int padding = base64.length() % 4;
        if (padding != 0) {
            String corrected = base64 + "=".repeat(4 - padding);
            log.info("Base64 corregido con padding: {} -> {}", base64.length(), corrected.length());
        }
    }

    /**
     * Resuelve la imagen captcha
     *
     * @param base64Image imagen
     * @return {@link String}
     */
    private String resolveCaptcha(String base64Image) {
        log.info("Resolviendo captcha con 2Captcha...");

        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new BcpException("Servicio 2Captcha no configurado",
                        "BCP_2CAPTCHA_NOT_CONFIGURED",
                        "Variable 2captcha.api.key no configurada en properties");
            }

            TwoCaptcha solver = new TwoCaptcha(apiKey);

            solver.setDefaultTimeout(Integer.parseInt(timeoutStr));
            solver.setRecaptchaTimeout(Integer.parseInt(timeoutStr));
            solver.setPollingInterval(Integer.parseInt(pollingStr));

            Normal captcha = new Normal();
            captcha.setBase64(base64Image);
            captcha.setCaseSensitive(false);
            captcha.setMinLen(4);  // Longitud mínima típica de captchas
            captcha.setMaxLen(6);  // Longitud máxima típica de captchas
            captcha.setNumeric(0); // 0 = no numérico, 1 = solo números, 2 = ambos

            log.info("Enviando captcha a 2Captcha (tamaño base64: {} bytes)", base64Image.length());

            solver.solve(captcha);

            String solution = captcha.getCode();

            if (solution == null || solution.trim().isEmpty()) {
                throw new BcpException("Solución de captcha vacía",
                        "BCP_CAPTCHA_EMPTY_SOLUTION",
                        "El servicio 2Captcha retornó una solución vacía");
            }

            log.info("Captcha resuelto por 2Captcha: {}", solution);

            return solution;

        } catch (Exception e) {
            log.error("Error resolviendo captcha con 2Captcha: {}", e.getMessage());
            throw new BcpException("Error al resolver captcha con 2Captcha",
                    "BCP_2CAPTCHA_ERROR",
                    "Error en el servicio 2Captcha: " + e.getMessage());
        }
    }

    /**
     * Ingresa la solucion del captacha
     *
     * @param page manejador de página
     * @param solution solución del captcha
     */
    private void enterCaptchaSolution(Page page, String solution) {
        log.info("Ingresando solución del captcha: {}", solution);

        try {
            Locator captchaInput = MetodsGeneric.waitForVisible(page, "input[name='bcp-input-0']", 5_000);
            captchaInput.fill("");
            MetodsGeneric.humanTypeText(captchaInput, solution, 120, 220); // más humano
            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page,500, 800);
            log.info("Solución del captcha ingresada exitosamente");
        } catch (Exception e) {
            log.error("Error ingresando solución del captcha: {}", e.getMessage());
            throw new BcpException("Error ingresando solución de captcha",
                    "BCP_CAPTCHA_INPUT_ERROR",
                    "Error al ingresar la solución del captcha");
        }
    }

    /**
     * Comprueba si el captacha fue aceptado
     *
     * @param page manejador de página
     * @return {@link boolean}
     */
    private boolean isCaptchaAccepted(Page page) {
        try {
            MetodsGeneric.randomWaitPage(page,1000, 1500);
            String selectorError = "bcp-alert p:has-text('El captcha ingresado es incorrecto.')";
            Locator alertaError = page.locator(selectorError);

            if (alertaError.count() > 0 && alertaError.first().isVisible()) {
                log.warn("Captcha rechazado: Se detectó el mensaje 'El captcha ingresado es incorrecto.'");
                return false;
            }
            if (!page.url().contains("tarjeta-sesion")) {
                log.info("Captcha aceptado: Ya no estamos en la página de login");
                return true;
            }

            log.info("No se detectaron errores de captcha visibles");
            return true;
        } catch (Exception e) {
            log.warn("Error verificando estado del captcha: {}", e.getMessage());
            return true;
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
            Locator loginButton = MetodsGeneric.waitForVisible(page, "//button//span[normalize-space()='Continuar']", 2_000);
            loginButton.click(new Locator.ClickOptions().setTimeout(2_000));
            log.info("Click en botón de login realizado");
            MetodsGeneric.randomWaitPage(page, 2500, 3500);
        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new BcpException("Error al hacer click en botón de login",
                    "BCP_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }
}