package com.raissa.rpa.service.impl.ibk;

import com.raissa.rpa.config.SvgDigitClassifier;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.exception.IbkException;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.ibk.IBKEmpresaService;
import com.raissa.rpa.service.ibk.IbkMenuService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class IBKEmpresaServiceImpl implements IBKEmpresaService {
    @Value("${banking.ibk.url}")
    private String ibkUrl;

    @Value("${2captcha.api.key}")
    private String apiKey;

    @Value("${2captcha.timeout}")
    private String timeoutStr;

    @Value("${2captcha.polling.interval}")
    private String pollingStr;

    @Value("${app.production:false}")
    private boolean isProduction;

    private final IbkMenuService ibkMenuService;

    private final Map<String, WebDriver> driverCache = new ConcurrentHashMap<>();

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login IBK");

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
            driver.get(ibkUrl);

            log.info("Navegando a: {}", ibkUrl);

            MetodsGeneric.randomWait(2000, 3000);

            // 1. ✅ Seleccionar tipo de documento (toggle + combo)
            String docSelector = credentials.get("codigoEmpresa");
            selectLoginDocumentType(driver, docSelector);

            MetodsGeneric.randomWait(2000, 3000);

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
            enterUserCode(driver, docSelector, usuario);

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

            enterPassword(driver, docSelector, claveAcceso);
            MetodsGeneric.randomWait(300, 500);

            // 3. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(driver);

            // 6. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = ibkMenuService.verifyLoginSuccess(driver);

            if (!loginSuccess) {
                throw new IbkException("Error en el login después de enviar formulario",
                        "IBK_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login exitoso");
            }

            // 6. ✅ ÉXITO - Almacenar driver y retornar resultado
            driverCache.put(transactionId, driver);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login IBK exitoso", true);

            log.info("Login IBK completado exitosamente");

            success = true;

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "IBK_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error genérico en login IBK: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "IBK_GENERIC_ERROR");
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
        log.info("Obteniendo saldo IBK, transactionId: {}", transactionId);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            if (!ibkMenuService.verifyLoginSuccess(driver)) {
                throw new IbkException("No se pudo navegar a cuentas",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            if (!ibkMenuService.closeCampaignPopupIfPresent(driver)) {
                log.info("No hay popup que cerrar");
            }

            // 1. ✅ Hacer clic en "consultas"
            if (!ibkMenuService.clickConsultas(driver)) {
                throw new IbkException("No se pudo navegar a saldos",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de saldos");
            }

            // 2. ✅ Esperar a que carguen los datos
            ibkMenuService.waitForAccountsToLoad(driver);

            // 3. ✅ Extraer datos de las cuentas
            List<Map<String, Object>> accounts = ibkMenuService.extractAccountsData(driver);

            // 4. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put("data", accounts);
            result.put("count", accounts.size());

            return result;
        } catch (Exception e) {
            log.error("Error obteniendo saldo IBK: {}", e.getMessage());

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos IBK, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 1. ✅ Navegar a la opción "Movimientos" del menú lateral
            if (!ibkMenuService.clickMovimientos(driver)) {
                throw new IbkException("No se pudo navegar a movimietnos",
                        "IBK_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de movimientos");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            ibkMenuService.waitForMovementsToLoad(driver);

            // 3. ✅ Seleccionar la cuenta específica
            if (!ibkMenuService.selectCuenta(driver, numeroCuenta)) {
                throw new IbkException("No se pudo seleccionar la cuenta",
                        "IBK_ACCOUNT_NOT_FOUND",
                        "La cuenta " + numeroCuenta + " no fue encontrada");
            }

            // 4. ✅ Configurar rango de fechas
            if (!ibkMenuService.setDateRange(driver, fechaInicio, fechaFin)) {
                throw new IbkException("No se pudo configurar el rango de fechas",
                        "IBK_DATE_RANGE_ERROR",
                        "Error al establecer fechas: " + fechaInicio + " - " + fechaFin);
            }

            // 5. ✅ Aplicar filtros y esperar resultados
            ibkMenuService.applyFilters(driver);
            ibkMenuService.waitForMovimientosToLoad(driver);

            // 6. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = ibkMenuService.extractMovimientosData(driver);

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
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> logout(String transactionId) {
        WebDriver driver = driverCache.get(transactionId);

        if (driver == null) {
            throw new BcpException("Sesión no encontrada",
                    "IBK_SESSION_NOT_FOUND",
                    "La sesión con ID " + transactionId + " no existe o ya fue cerrada");
        }

        try {
            log.info("Iniciando proceso de logout para transactionId: {}", transactionId);

            // 1. ✅ ABRIR EL DROPDOWN DE PERFIL (si no está visible)
            ibkMenuService.openProfileDropdown(driver);

            // 2. ✅ HACER CLICK EN "CERRAR SESIÓN"
            ibkMenuService.clickLogoutButton(driver);

            // 3. ✅ MODAL CUANDO SE MUESTRA
            ibkMenuService.handleNpsSurvey(driver);

            // 4. ✅ VERIFICAR QUE EL LOGOUT FUE EXITOSO
            boolean logoutSuccess = ibkMenuService.verifyLogoutSuccess(driver);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            }

            // 5. ✅ CERRAR EL DRIVER
            driver.quit();
            log.debug("Driver cerrado exitosamente");

        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());

            driver.quit();

            throw new BcpException("Error en logout", "IBK_LOGOUT_ERROR", e.getMessage());
        } finally {
            driverCache.remove(transactionId);
            log.info("Sesión {} removida del cache", transactionId);
        }

        return ResponseGeneric.buildSuccessResponse(transactionId, "Sesión cerrada exitosamente", true);
    }

    /**
     * Selecciona metodo de ingreso de credenciales
     *
     * @param driver manejador de pagina
     * @param selectorValue Valor para seleccioanr metodo de ingreso
     */
    private void selectLoginDocumentType(WebDriver driver, String selectorValue) {
        if (selectorValue == null || selectorValue.isBlank()) {
            return; // DNI queda como default
        }

        String normalized = selectorValue.trim().toUpperCase(Locale.ROOT);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

        switch (normalized) {
            case "TIE":
                // Cambiar a la pestaña TIE
                WebElement tieToggle = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//mat-button-toggle[.//span[contains(normalize-space(.),'TIE')]]//button")
                ));
                tieToggle.click();
                break;

            case "DNI":
                ensureDocIdentidadToggle(wait);
                break;

            case "CE":
                ensureDocIdentidadToggle(wait);
                selectMatOption(wait, "CE");
                break;

            case "PAS":
                ensureDocIdentidadToggle(wait);
                selectMatOption(wait, "Pasaporte");
                break;

            default:
                ensureDocIdentidadToggle(wait);
                break;
        }
    }

    /**
     * Hace clic en la pestaña de documento
     * @param wait tiempo de espera para ver si responde el elemento de la pagina
     */
    private void ensureDocIdentidadToggle(WebDriverWait wait) {
        WebElement docToggle = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//mat-button-toggle[.//span[contains(normalize-space(.),'Doc. Identidad')]]//button")
        ));
        if (!Boolean.parseBoolean(docToggle.getAttribute("aria-pressed"))) {
            docToggle.click();
        }
    }

    /**
     *  Selecciona la opcion de documento correspondiente
     *
     * @param wait  tiempo de espera para ver si responde el elemento de la página
     * @param optionText texto normalizado para obtener valor de seleccion
     */
    private void selectMatOption(WebDriverWait wait, String optionText) {
        WebElement matSelect = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("mat-select[data-test='cmbTypeDoc']")
        ));
        matSelect.click();

        WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(String.format("//mat-option//span[contains(normalize-space(.),'%s')]", optionText))
        ));
        option.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector(".cdk-overlay-pane .mat-mdc-select-panel")
        ));
    }

    /**
     * Ingresa el código del usuario en este caso número de tarjeta
     *
     * @param driver manejador de pagina
     * @param docSelector para saber en qué objeto ingresará valores de logueo
     * @param userValue código de usuario o número de tarjeta
     */
    private void enterUserCode(WebDriver driver, String docSelector, String userValue) {
        log.info("Ingresando tarjeta/código de usuario...");

        try {
            if (userValue == null || userValue.isBlank()) {
                throw new BcpException("El identificador de usuario no puede estar vacío",
                        "IBK_EMPTY_USER",
                        "Ingresa el usuario o número de documento para continuar");
            }

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
            String normalizedSelector = docSelector == null ? "" : docSelector.trim().toUpperCase(Locale.ROOT);

            if ("TIE".equals(normalizedSelector)) {
                By tieInputLocator = By.cssSelector("input#txtTie[data-test='txtTie']");
                WebElement tieInput = wait.until(ExpectedConditions.visibilityOfElementLocated(tieInputLocator));
                tieInput.clear();
                MetodsGeneric.humanTypeText(tieInput, userValue.trim());
            } else {
                By docInputLocator = By.cssSelector("input#login-doc[data-test='txtNumDoc']");
                WebElement docInput = wait.until(ExpectedConditions.visibilityOfElementLocated(docInputLocator));
                docInput.clear();
                MetodsGeneric.humanTypeText(docInput, userValue.trim());
            }
        } catch (TimeoutException e) {
            throw BcpException.elementNotFound("input login", "input user");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "IBK_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            throw new BcpException("Error al aplicar tabs de navegación",
                    "IBK_NAVIGATION_ERROR",
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
    private void enterPassword(WebDriver driver, String docSelector, String password) {
        log.info("Ingresando clave de {} dígitos...", password.length());

        try {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        String normalizedSelector = docSelector == null ? "" : docSelector.trim().toUpperCase(Locale.ROOT);

        if ("TIE".equals(normalizedSelector)) {
            By tiePasswordLocator = By.cssSelector("input#passwordTie[data-test='txtPassword']");
            WebElement tiePasswordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(tiePasswordLocator));

            tiePasswordInput.click();
            MetodsGeneric.randomWait(200, 350);

            Map<String, WebElement> keyMap = mapVirtualKeyboard(driver);

            for (char ch : password.toCharArray()) {
                String key = String.valueOf(ch);
                WebElement keyButton = keyMap.get(key.toUpperCase(Locale.ROOT));
                if (keyButton == null) {
                    throw new IbkException("Carácter no disponible en teclado virtual: " + ch,
                            "IBK_KEY_NOT_FOUND",
                            "El teclado no contiene la tecla " + ch);
                }
                clickVirtualKey(driver, keyButton, key);
            }

            log.info("Clave TIE ingresada exitosamente mediante teclado virtual");

        } else {
            By docPasswordLocator = By.cssSelector("input#passwordDoc[data-test='txtPassword']");
            WebElement docPasswordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(docPasswordLocator));

            if ("true".equalsIgnoreCase(docPasswordInput.getAttribute("readonly"))) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('readonly');", docPasswordInput);
            }

            docPasswordInput.click();
            docPasswordInput.clear();
            MetodsGeneric.humanTypeText(docPasswordInput, password.trim());

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
     * @param driver manejador de página
     * @return {@link Map}
     */
    private Map<String, WebElement> mapVirtualKeyboard(WebDriver driver) {
        log.info("Mapeando teclado virtual del IBK...");

        Map<String, WebElement> keyMap = new HashMap<>();

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
            WebElement keyboard = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("ibk-keyboard[data-test='tblKeyboard']")
            ));

            List<WebElement> keys = keyboard.findElements(By.cssSelector(
                    "ibk-button.ibk-keyboard__btn:not(.ibk-keyboard__btn--reset):not(.ibk-keyboard__btn--delete) > button"
            ));

            for (WebElement button : keys) {
                String dataTest = button.getDomAttribute("data-test");
                if ("btnLoad".equalsIgnoreCase(dataTest)) {
                    continue;
                }

                String rawLabel = processKeyboardKey(driver, button);
                if (rawLabel == null || rawLabel.isBlank()) {
                    log.debug("Botón omitido por carecer de etiqueta resoluble: {}", button);
                    continue;
                }

                String normalizedKey = rawLabel.trim().toUpperCase(Locale.ROOT);
                WebElement previous = keyMap.putIfAbsent(normalizedKey, button);
                if (previous != null) {
                    log.warn("Clave duplicada en teclado virtual: {}", normalizedKey);
                }
            }

            if (keyMap.isEmpty()) {
                throw new IllegalStateException("No se pudo mapear ninguna tecla del teclado virtual.");
            }

            return keyMap;

        } catch (TimeoutException e) {
            log.error("Timeout buscando teclado virtual: {}", e.getMessage(), e);
            throw new BcpException("Timeout teclado virtual",
                    "IBK_KEYBOARD_TIMEOUT",
                    "No se pudo encontrar el teclado virtual en el tiempo esperado");

        } catch (Exception e) {
            log.error("Error mapeando teclado virtual: {}", e.getMessage(), e);
            throw new BcpException("Error mapeando teclado virtual",
                    "IBK_KEYBOARD_MAPPING_ERROR",
                    "Error al procesar el teclado del banco: " + e.getMessage());
        }
    }

    private String processKeyboardKey(WebDriver driver, WebElement button) {
        try {
            String label = firstNonBlankAttribute(button, "aria-label", "data-key", "value");
            if (!label.isBlank()) {
                return label.trim();
            }

            Optional<String> svgValue = resolveKeyFromSvg(button);
            if (svgValue.isPresent()) {
                return svgValue.get();
            }

            String fallback = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent && arguments[0].textContent.trim();", button);
            return Optional.ofNullable(fallback).orElse("");

        } catch (Exception e) {
            log.warn("Error procesando tecla: {}", e.getMessage(), e);
            return "";
        }
    }

    private Optional<String> resolveKeyFromSvg(WebElement button) {
        List<WebElement> paths = button.findElements(By.cssSelector("svg path[d]"));
        for (WebElement path : paths) {
            String dAttribute = path.getDomAttribute("d");
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

    private String firstNonBlankAttribute(WebElement element, String... attributes) {
        for (String attribute : attributes) {
            String value = element.getDomAttribute(attribute);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * Hacer clic en cada dígito de la contraseña
     *
     * @param driver manejador de página
     * @param keyButton indice de tecla
     * @param keyLabel valor del boton
     */
    private void clickVirtualKey(WebDriver driver, WebElement keyButton, String keyLabel) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.elementToBeClickable(keyButton));
            keyButton.click();
            log.debug("Click en tecla '{}'", keyLabel);

            MetodsGeneric.randomWait(300, 500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "IBK_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error haciendo click en tecla '{}': {}", keyLabel, e.getMessage());
            throw new BcpException("Error al hacer click en tecla virtual",
                    "IBK_KEY_CLICK_ERROR",
                    "Error al interactuar con el teclado del banco");
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
            WebElement loginButton = waitForElement(driver,By.xpath("//button//span[normalize-space()='Iniciar sesión']"));

            loginButton.click();
            log.info("Click en botón de login realizado");

            MetodsGeneric.randomWait(2500, 3500);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcpException("Interrupción durante la navegación",
                    "IBK_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new BcpException("Error al hacer click en botón de login",
                    "IBK_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }
}
