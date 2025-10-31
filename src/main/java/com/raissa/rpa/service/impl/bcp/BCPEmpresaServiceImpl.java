package com.raissa.rpa.service.impl.bcp;

import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.bcp.BCPEmpresaService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Normal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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

    @Value("${2captcha.polling.maxIntentos}")
    private String cantIntentos;

    @Value("${app.production:false}")
    private boolean isProduction;

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

            // 5. ✅ Manejar el modal móvil si está abierto
            handleMobileModal(driver);

            // 6. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = verifyLoginSuccess(driver);

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

            if (!isOnAccountsPage(driver)) {
                throw new BcpException("No se pudo navegar a cuentas",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 1. ✅ Hacer clic en el tab "Cuentas"
            clickAccountsTab(driver);

            // 2. ✅ Esperar a que carguen los datos
            waitForAccountsToLoad(driver);

            // 3. ✅ Extraer datos de las cuentas
            List<Map<String, Object>> accounts = extractAccountsData(driver);

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
            if (!navigateToResumen(driver)) {
                throw new BcpException("No se pudo navegar a resumen",
                        "BCP_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de resumen");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            waitForResumenAccountsToLoad(driver);

            // 3. ✅ Seleccionar la cuenta específica
            if (!selectCuenta(driver, numeroCuenta)) {
                throw new BcpException("No se pudo seleccionar la cuenta",
                        "BCP_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            if (!setDateRange(driver, fechaInicio, fechaFin)) {
                throw new BcpException("No se pudo configurar el rango de fechas",
                        "BCP_DATE_RANGE_ERROR",
                        "Error al establecer fechas: " + fechaInicio + " - " + fechaFin);
            }

            // 5. ✅ Aplicar filtros y esperar resultados
            applyFilters(driver);
            waitForMovimientosToLoad(driver);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = extractMovimientosData(driver);

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
            openProfileDropdown(driver);

            // 2. ✅ HACER CLICK EN "CERRAR SESIÓN"
            clickLogoutButton(driver);

            // 3. ✅ MANEJAR POSIBLE ENCUESTA NPS (si aparece)
            handleNpsSurvey(driver);

            // 4. ✅ VERIFICAR QUE EL LOGOUT FUE EXITOSO
            boolean logoutSuccess = verifyLogoutSuccess(driver);

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
            List<WebElement> keyboardKeys = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("bcp-keyboard-key[index]"))
            );

            log.info("Teclas encontradas: {}", keyboardKeys.size());

            if (keyboardKeys.isEmpty()) {
                throw new BcpException("Teclado virtual no encontrado",
                        "BCP_KEYBOARD_NOT_FOUND",
                        "No se encontraron teclas del teclado virtual");
            }

            int processedCount = 0;
            for (WebElement key : keyboardKeys) {
                if (processKeyboardKey(key, keyMap)) {
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
            String xpath = "//bcp-img[@class='bcp-img-host hydrated']/img[@height='48' and @width='127' and contains(@src, 'data:image')]";

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement captchaImg = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath))
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

    /**
     * Verifica si el logueo fue exitoso
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean verifyLoginSuccess(WebDriver driver) {
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
     * Maneja excepciones de interrupción de manera consistente
     *
     * @param e error de interrupcion
     */
    private void handleInterruptedException(InterruptedException e) {
        log.error("Interrupción durante la navegación: {}", e.getMessage());
        Thread.currentThread().interrupt();
        throw new BcpException("Interrupción durante la navegación",
                "BCP_NAVIGATION_INTERRUPTED",
                "El proceso fue interrumpido durante la navegación");
    }

    /**
     * Abre el desplegable para cerrar sesion
     *
     * @param driver manejador de pagina
     */
    private void openProfileDropdown(WebDriver driver) {
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
     * Hace clic en el boton de cerrar sesion
     *
     * @param driver manejador de pagina
     */
    private void clickLogoutButton(WebDriver driver) {
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

    /**
     * Verifica si se abrio encuesta
     *
     * @param driver manejador de pagina
     */
    private void handleNpsSurvey(WebDriver driver) {
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
     * Verifica que se haya cerrado la sesion
     *
     * @param driver manejador de pagina
     * @return {@link boolean}
     */
    private boolean verifyLogoutSuccess(WebDriver driver) {
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

    /**
     * Metodo para detectar y cerrar el modal móvil si está abierto
     *
     * @param driver manejador de pagina
     */
    private void handleMobileModal(WebDriver driver) {
        try {
            // Esperar un momento para que el DOM se estabilice
            MetodsGeneric.randomWait(1000, 1500);

            // Verificar si el modal está presente y visible
            List<WebElement> modals = driver.findElements(By.cssSelector("bcp-mobile-modal .bcp-modal-host-4-25-0.show"));

            if (!modals.isEmpty() && modals.get(0).isDisplayed()) {
                log.info("Modal móvil detectado, intentando cerrarlo...");

                // Intentar localizar el botón de cerrar con diferentes selectores
                WebElement closeButton = null;

                // Primero intentar con el selector más específico
                try {
                    closeButton = driver.findElement(By.cssSelector("#bcp-modal-2 .close-button"));
                } catch (NoSuchElementException e) {
                    // Si falla, intentar con selector más genérico
                    try {
                        closeButton = driver.findElement(By.cssSelector(".bcp-ffw-modal-header-close .close-button"));
                    } catch (NoSuchElementException e2) {
                        // Último intento con selector más simple
                        closeButton = driver.findElement(By.cssSelector(".close-button"));
                    }
                }

                // Verificar que el botón esté visible y habilitado
                if (closeButton != null && closeButton.isDisplayed() && closeButton.isEnabled()) {
                    // Hacer clic en el botón de cerrar
                    closeButton.click();
                    log.info("Modal cerrado exitosamente");

                    // Esperar a que el modal desaparezca
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                    wait.until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector("bcp-mobile-modal .bcp-modal-host-4-25-0.show")));

                } else {
                    log.warn("Botón de cerrar no está disponible para hacer clic");
                }
            } else {
                log.info("No se detectó modal móvil abierto");
            }

        } catch (NoSuchElementException e) {
            log.info("No se encontró el modal móvil: {}", e.getMessage());
        } catch (TimeoutException e) {
            log.warn("Timeout esperando a que el modal se cierre: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error manejando el modal móvil: {}", e.getMessage());
        }
    }

    /**
     * Selecciona tab de cuentas
     *
     * @param driver manejador de página
     */
    private void clickAccountsTab(WebDriver driver) {
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
            log.debug("Interrupción al intentar realizar clic con xpath {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("XPath específico también falló: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si se encuentra en el tab de cuentas
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean isOnAccountsPage(WebDriver driver) {
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
     * Espera a que cargue la interfaz con las cuentas
     *
     * @param driver manejador de página
     */
    private void waitForAccountsToLoad(WebDriver driver) {
        try {
            log.debug("Esperando a que carguen los datos de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//bcp-title-9nbaaa//h1[normalize-space()='Cuentas']")
            ));

            Thread.sleep(500);
        } catch (InterruptedException e) {
            handleInterruptedException(e);
            log.debug("Interrupción al esperar que carguen datos de cuenta {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error esperando carga de cuentas: {}", e.getMessage());
        }
    }

    /**
     * Espera a que cargue la interfaz de resumen de cuentas
     *
     * @param driver manejador de página
     */
    private void waitForResumenAccountsToLoad(WebDriver driver) {
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
            log.debug("Interrupción al esperar que carguen datos de resumen de cuenta {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error esperando carga de resumen de cuentas: {}", e.getMessage());
        }
    }

    /**
     * Metodo de extracción para saldos de cuentas
     * @param driver manejador de página
     * @return {@link List<Map>} lista de saldos y cuentas
     */
    private List<Map<String, Object>> extractAccountsData(WebDriver driver) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        try {
            log.debug("Extrayendo datos reales de cuentas...");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            WebElement dataTable = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("bcp-data-table-9nbaaa")
            ));

            List<WebElement> accountRows = dataTable.findElements(By.xpath(
                    ".//bcp-table-row-9nbaaa[contains(@index, '0000')]"
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
            WebElement saldoDisponibleElement = row.findElement(By.xpath(
                    ".//bcp-table-col-9nbaaa[@index='4']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi text']"
            ));
            String saldoDisponibleStr = saldoDisponibleElement.getText().trim();
            double saldoDisponible = MetodsGeneric.parseSaldo(saldoDisponibleStr);
            account.put(Constantes.KEY_SALDO_DISP, saldoDisponible);

            // 4. Saldo contable
            WebElement saldoContableElement = row.findElement(By.xpath(
                    ".//bcp-table-col-9nbaaa[@index='6']//bcp-paragraph-9nbaaa[@family='demi']/p[@class='paragraph-sm bcp-font-demi text']"
            ));
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
     * Hacer clic en la opción "Resumen" del menú lateral
     *
     * @param driver manejador de página
     * @return {@link boolean}
     */
    private boolean navigateToResumen(WebDriver driver) {
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

    /**
     * Se ubica en una cuenta especifica de la lista de cuentas disponibles
     * @param driver manejador de página
     * @param numeroCuenta numero de cuenta
     * @return {@link boolean}
     */
    private boolean selectCuenta(WebDriver driver, String numeroCuenta) {
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

    private boolean setDateRange(WebDriver driver, String fechaInicio, String fechaFin) {
        log.info("Iniciando carga de rango de fechas");
        try {
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

            Thread.sleep(500);

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

    private void applyFilters(WebDriver driver) {
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

    private void waitForMovimientosToLoad(WebDriver driver) {
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

    private List<Map<String, Object>> extractMovimientosData(WebDriver driver) {
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
}