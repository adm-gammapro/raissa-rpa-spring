package com.raissa.rpa.service.impl.bcp;

import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.bcp.BCPEmpresaService;
import com.raissa.rpa.service.bcp.BcpMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Normal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BCPEmpresaServiceImpl implements BCPEmpresaService {
    @Value("${banking.bcp.url}")
    private String bcpUrl;

    @Value("${2captcha.api.key}")
    private String apiKey;

    @Value("${2captcha.timeout}")
    private String timeoutStr;

    @Value("${2captcha.polling.interval}")
    private String pollingStr;

    @Value("${app.production:false}")
    private boolean isProduction;

    private final BcpMenuService bcpMenuService;

    private final Map<String, WebDriver> driverCache = new ConcurrentHashMap<>();

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login BCP");

        WebDriver driver = null;
        boolean success = false;
        Map<String, Object> result;

        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--window-size=1400,1000");

            // Disimula automatización
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);

            // User-Agent realista
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

            // crea otro perfil
            options.addArguments("--profile-directory=Default");

            if (isProduction) {
                options.addArguments("--headless=new");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--font-render-hinting=medium");
                options.addArguments("--disable-dev-shm-usage");
            }

            options.addArguments("--lang=es-PE");

            driver = new ChromeDriver(options);
            driver.get(bcpUrl);

            log.info("Navegando a: {}", bcpUrl);

            MetodsGeneric.randomWait(2000, 3000);

            // 1. ✅ Ingresar código de usuario
            enterUserCode(driver, credentials.get("codigoUsuario"));

            // 2. ✅ INGRESAR CLAVE CON TECLADO VIRTUAL
            String claveAcceso = credentials.get("claveAcceso");
            if (claveAcceso == null || claveAcceso.length() != 6) {
                throw new BcpException("Clave de acceso debe tener 6 dígitos",
                        "BCP_INVALID_PASSWORD_LENGTH",
                        "La clave debe tener exactamente 6 dígitos");
            }

            enterPassword(driver, claveAcceso);
            MetodsGeneric.randomWait(300, 500);

            // 3. ✅ MANEJAR CAPTCHA
            handleCaptcha(driver);

            // 4. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(driver);

            // 6. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = bcpMenuService.verifyLoginSuccess(driver);

            if (!loginSuccess) {
                throw new BcpException("Error en el login después de enviar formulario",
                        "BCP_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login exitoso");
            }

            // 6. ✅ ÉXITO - Almacenar driver y retornar resultado
            driverCache.put(transactionId, driver);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login BCP exitoso", true);

            log.info("Login BCP completado exitosamente");

            success = true;

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error genérico en login BCP: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "BCP_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());

            return errorResult;
        } finally {
            if (driver != null && !success) {
                try {
                    driver.quit();
                    log.info("Driver cerrado debido a error");
                } catch (Exception e) {
                    log.warn("Error al cerrar driver: {}", e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> obtenerSaldo(String transactionId) {
        Map<String, Object> result;
        log.info("Obteniendo saldo BCP, transactionId: {}", transactionId);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 0. ✅ Manejar el modal móvil si está abierto
            bcpMenuService.handleMobileModal(driver);

            if (!bcpMenuService.isOnAccountsPage(driver)) {
                throw new BcpException("No se pudo navegar a cuentas",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 1. ✅ Hacer clic en el tab "Cuentas"
            bcpMenuService.clickAccountsTab(driver);

            // 2. ✅ Esperar a que carguen los datos
            bcpMenuService.waitForAccountsToLoad(driver);

            // 3. ✅ Extraer datos de las cuentas
            List<Map<String, Object>> accounts = bcpMenuService.extractAccountsData(driver);

            // 4. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put("data", accounts);
            result.put("count", accounts.size());

            return result;

        } catch (Exception e) {
            log.error("Error obteniendo saldo BCP: {}", e.getMessage());

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 1. ✅ Navegar a la opción "Resumen" del menú lateral
            if (!bcpMenuService.navigateToResumen(driver)) {
                throw new BcpException("No se pudo navegar a resumen",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de resumen");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            bcpMenuService.waitForResumenAccountsToLoad(driver);

            // 3. ✅ Seleccionar la cuenta específica
            if (!bcpMenuService.selectCuenta(driver, numeroCuenta)) {
                throw new BcpException("No se pudo seleccionar la cuenta",
                        "BCP_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            if (!bcpMenuService.setDateRange(driver, fechaInicio, fechaFin)) {
                throw new BcpException("No se pudo configurar el rango de fechas",
                        "BCP_DATE_RANGE_ERROR",
                        "Error al establecer fechas: " + fechaInicio + " - " + fechaFin);
            }

            // 5. ✅ Aplicar filtros y esperar resultados
            bcpMenuService.applyFilters(driver);
            bcpMenuService.waitForMovimientosToLoad(driver);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bcpMenuService.extractMovimientosData(driver);

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
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientosHistorico(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 1. ✅ Navegar a la opción "Resumen" del menú lateral
            if (!bcpMenuService.navigateToResumen(driver)) {
                throw new BcpException("No se pudo navegar a resumen",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de resumen");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            bcpMenuService.waitForResumenAccountsToLoad(driver);

            // 3. ✅ Seleccionar la cuenta específica
            if (!bcpMenuService.selectCuentaHistorico(driver, numeroCuenta)) {
                throw new BcpException("No se pudo seleccionar la cuenta",
                        "BCP_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            if (!bcpMenuService.setDateRange(driver, fechaInicio, fechaFin)) {
                throw new BcpException("No se pudo configurar el rango de fechas",
                        "BCP_DATE_RANGE_ERROR",
                        "Error al establecer fechas: " + fechaInicio + " - " + fechaFin);
            }

            // 5. ✅ Aplicar filtros y esperar resultados
            bcpMenuService.applyFiltersHistorico(driver);
            bcpMenuService.waitForMovimientosToLoad(driver);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bcpMenuService.extractMovimientosData(driver);

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
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> logout(String transactionId) {
        WebDriver driver = driverCache.get(transactionId);

        if (driver == null) {
            throw new BcpException("Sesión no encontrada",
                    "BCP_SESSION_NOT_FOUND",
                    "La sesión con ID " + transactionId + " no existe o ya fue cerrada");
        }

        try {
            log.info("Iniciando proceso de logout para transactionId: {}", transactionId);

            // 1. ✅ ABRIR EL DROPDOWN DE PERFIL (si no está visible)
            bcpMenuService.openProfileDropdown(driver);

            // 2. ✅ HACER CLICK EN "CERRAR SESIÓN"
            bcpMenuService.clickLogoutButton(driver);

            // 3. ✅ MANEJAR POSIBLE ENCUESTA NPS (si aparece)
            bcpMenuService.handleNpsSurvey(driver);

            // 4. ✅ VERIFICAR QUE EL LOGOUT FUE EXITOSO
            boolean logoutSuccess = bcpMenuService.verifyLogoutSuccess(driver);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            }

            // 5. ✅ CERRAR EL DRIVER
            driver.quit();
            log.debug("Driver cerrado exitosamente");

        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());

            driver.quit();

            throw new BcpException("Error en logout", "BCP_LOGOUT_ERROR", e.getMessage());
        } finally {
            driverCache.remove(transactionId);
            log.info("Sesión {} removida del cache", transactionId);
        }

        return ResponseGeneric.buildSuccessResponse(transactionId, "Sesión cerrada exitosamente", true);
    }

    /**
     * Ingresa el código del usuario en este caso número de tarjeta
     *
     * @param driver manejador de pagina
     * @param userCode código de usuario o número de tarjeta
     */
    private void enterUserCode(WebDriver driver, String userCode) {
        log.info("Ingresando tarjeta/código de usuario...");

        try {
            WebElement inputLogin = waitForElement(driver, By.cssSelector("input[name='ciam-input-card']"));

            inputLogin.clear();
            MetodsGeneric.humanTypeText(inputLogin, userCode);

            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(300, 500);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(300, 500);

            log.info("Usuario ingresado y tabs aplicados");

        } catch (TimeoutException e) {
            throw BcpException.elementNotFound("input login", "input user");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            throw new BcpException("Error al aplicar tabs de navegación",
                    "BCP_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ubica un elemento en la página
     *
     * @param driver manejador de página
     * @param locator etiqueta a buscar
     * @return {@link WebElement}
     */
    private WebElement waitForElement(WebDriver driver, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Ingresa el password en la caja de contraseña
     *
     * @param driver manejador de página
     * @param password contraseña a ingresar
     */
    private void enterPassword(WebDriver driver, String password) {
        log.info("Ingresando clave de {} dígitos...", password.length());

        try {
            Map<String, Integer> keyMap = mapVirtualKeyboard(driver);

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
                String digitStr = String.valueOf(digit);
                Integer keyIndex = keyMap.get(digitStr);

                log.info("Ingresando dígito: {} -> tecla índice: {}", digit, keyIndex);
                clickVirtualKey(driver, keyIndex);
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
     * @param driver manejador de página
     * @return {@link Map}
     */
    private Map<String, Integer> mapVirtualKeyboard(WebDriver driver) {
        log.info("Mapeando teclado virtual del BCP...");

        Map<String, Integer> keyMap = new LinkedHashMap<>();

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            WebElement passwordBox = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("bcp-input-password .input-password")
            ));
            passwordBox.click();

            List<WebElement> keyboardKeys = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(
                            By.cssSelector("bcp-input-password bcp-keyboard-key[index]")
                    )
            );

            log.info("Teclas encontradas: {}", keyboardKeys.size());

            if (keyboardKeys.isEmpty()) {
                throw new BcpException("Teclado virtual no encontrado",
                        "BCP_KEYBOARD_NOT_FOUND",
                        "No se encontraron teclas del teclado virtual");
            }

            int processedCount = 0;
            for (WebElement key : keyboardKeys) {
                String idx = key.getDomAttribute("index");
                WebElement digit = key.findElement(By.cssSelector(".digit-number"));
                String label = digit.getText().trim();

                if (!label.isEmpty() && idx != null) {
                    keyMap.put(label, Integer.parseInt(idx));
                    processedCount++;
                }
            }

            log.info("Teclas procesadas exitosamente: {}/{}", processedCount, keyboardKeys.size());
            log.debug("Teclado virtual mapeado: {}", keyMap);

            validateKeyMapping(keyMap);

            return keyMap;

        } catch (TimeoutException e) {
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
     * Procesa una tecla y devuelve true si fue exitoso
     *
     * @param key elemento de página
     * @param keyMap mapa de teclas
     * @return {@link boolean}
     */
    private boolean processKeyboardKey(WebElement key, Map<String, Integer> keyMap) {
        try {
            if (!key.isDisplayed()) {
                log.debug("Tecla no visible, omitiendo...");
                return false;
            }

            Optional<Integer> indexOpt = extractKeyIndex(key);
            Optional<String> digitOpt = extractKeyDigit(key);

            if (indexOpt.isPresent() && digitOpt.isPresent()) {
                String digit = digitOpt.get();
                Integer index = indexOpt.get();

                if (keyMap.containsKey(digit)) {
                    log.warn("Tecla duplicada encontrada: {} -> {} (ya existía: {})",
                            digit, index, keyMap.get(digit));
                    return false;
                }

                keyMap.put(digit, index);
                log.debug("Tecla mapeada: {} -> {}", digit, index);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Error procesando tecla: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el índice de la tecla desde el atributo 'index'
     *
     * @param key elemento de página
     * @return {@link Optional<Integer>}
     */
    private Optional<Integer> extractKeyIndex(WebElement key) {
        try {
            String indexStr = key.getDomAttribute("index");
            if (indexStr == null || indexStr.trim().isEmpty()) {
                log.debug("Tecla sin atributo index");
                return Optional.empty();
            }
            return Optional.of(Integer.parseInt(indexStr.trim()));
        } catch (NumberFormatException e) {
            log.warn("Atributo index no es un número válido");
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error obteniendo índice de tecla: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extrae el dígito de la tecla desde el elemento de título
     *
     * @param key elemento de página
     * @return {@link Optional<String>}
     */
    private Optional<String> extractKeyDigit(WebElement key) {
        try {
            WebElement titleElement = key.findElement(By.cssSelector("bcp-title h3"));
            String digit = titleElement.getText().trim();
            return digit.isEmpty() ? Optional.empty() : Optional.of(digit);
        } catch (Exception e) {
            log.warn("Error obteniendo dígito de tecla: {}", e.getMessage());
            return Optional.empty();
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
     * @param driver manejador de página
     * @param keyIndex indice de tecla
     */
    private void clickVirtualKey(WebDriver driver, int keyIndex) {
        try {
            String selector = String.format("bcp-keyboard-key[index='%d']", keyIndex);
            WebElement key = driver.findElement(By.cssSelector(selector));

            key.click();
            log.debug("Click en tecla índice: {}", keyIndex);

            MetodsGeneric.randomWait(300, 500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
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
     * @param driver manejador de página
     */
    private void handleCaptcha(WebDriver driver) {
        log.info("Manejando captcha del BCP...");

        try {
            // 1. Obtener imagen del captcha (con reintentos)
            String captchaImageBase64 = extractCaptchaBase64(driver);

            // 2. Resolver el captcha
            String captchaSolution = resolveCaptcha(captchaImageBase64);

            // 3. Ingresar la solución
            enterCaptchaSolution(driver, captchaSolution);

            // 5. Verificar si el captcha fue aceptado
            if (!isCaptchaAccepted(driver)) {
                throw new BcpException("Solución de captcha rechazada",
                        "BCP_CAPTCHA_REJECTED",
                        "La solución del captcha no fue aceptada");
            }

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
     * @param driver WebDriver
     * @return String base64 de la imagen
     */
    private String extractCaptchaBase64(WebDriver driver) {
        log.info("Extrayendo captcha BCP...");

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

            String css = "bcp-captcha bcp-img img[src^='data:image']";

            WebElement captchaImg = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css))
            );

            String src = captchaImg.getDomAttribute("src");
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
     * @param driver manejador de página
     * @param solution solución del captcha
     */
    private void enterCaptchaSolution(WebDriver driver, String solution) {
        log.info("Ingresando solución del captcha: {}", solution);

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

            WebElement captchaInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("input[name='bcp-input-0']")
                    )
            );

            captchaInput.clear();
            MetodsGeneric.humanTypeText(captchaInput, solution);

            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(500, 800);

            log.info("Solución del captcha ingresada exitosamente");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
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
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean isCaptchaAccepted(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));

            boolean hasError = wait.until(driverVal ->
                    driverVal.findElements(
                            By.cssSelector(".bcp-ffw-form-control:invalid, .error, .text-danger")
                    ).isEmpty()
            );

            return !hasError;

        } catch (TimeoutException e) {
            log.debug("No se encontraron errores de captcha (timeout esperado)");
            return true;
        } catch (Exception e) {
            log.warn("Error verificando estado del captcha: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Hace clic en el boton de ingresar
     *
     * @param driver manejador de página
     */
    private void clickLoginButton(WebDriver driver) {
        log.info("Haciendo click en botón de login...");

        try {
            WebElement loginButton = waitForElement(driver,By.xpath("//button//span[normalize-space()='Continuar']"));

            loginButton.click();
            log.info("Click en botón de login realizado");

            MetodsGeneric.randomWait(2500, 3500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "BCP_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new BcpException("Error al hacer click en botón de login",
                    "BCP_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }
}