package com.raissa.rpa.service.impl.bbva;

import com.raissa.rpa.exception.BbvaException;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.bbva.BBVAEmpresaService;
import com.raissa.rpa.service.bbva.BbvaMenuService;
import com.raissa.rpa.service.commons.NavigatorService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BBVAEmpresaServiceImpl implements BBVAEmpresaService {
    @Value("${banking.bbva.url}")
    private String bbvaUrl;

    private final NavigatorService navigatorService;
    private final BbvaMenuService bbvaMenuService;

    private final Map<String, WebDriver> driverCache = new ConcurrentHashMap<>();

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login BBVA");

        WebDriver driver = null;
        boolean success = false;
        Map<String, Object> result;

        try {
            driver = navigatorService.iniciarNavegador();

            driver.get(bbvaUrl);

            log.info("Navegando a: {}", bbvaUrl);

            MetodsGeneric.randomWait(1000, 3000);

            // 1. ✅ Ingresar código de empresa
            enterEnterpriseCode(driver, credentials.get("codigoEmpresa"));

            // 2. ✅ Ingresar código de usuario
            enterUserCode(driver, credentials.get("codigoUsuario"));

            // 3. ✅ INGRESAR CLAVE CON TECLADO VIRTUAL
            enterPassword(driver, credentials.get("claveAcceso"));

            // 5. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(driver);

            // 6. ✅ VERIFICAR si hay modal y cerrarlo
            closeModalIfPresent(driver);

            MetodsGeneric.randomWait(1000, 2000);

            // 7. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = bbvaMenuService.isMenuVisible(driver);

            if (!loginSuccess) {
                throw new BbvaException("Error en el login después de enviar formulario",
                        "BBVA_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login");
            }

            // 8. ✅ ÉXITO - Almacenar driver y retornar resultado
            driverCache.put(transactionId, driver);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login BBVA exitoso", true);

            log.info("Login BBVA completado exitosamente");

            success = true;

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error genérico en login BBVA: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "BBVA_GENERIC_ERROR");
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

            boolean clickPosicionGlobal = bbvaMenuService.clickCuentas(driver);
            if (!clickPosicionGlobal) {
                throw new BbvaException("No se pudo navegar a cuentas",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            MetodsGeneric.randomWait(8000, 10000);

            boolean clickPosicionGlobalOnline = bbvaMenuService.clickPosicionGlobalOnline(driver);
            if (!clickPosicionGlobalOnline) {
                throw new BbvaException("No se pudo navegar a posicion global",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de posicion global");
            }
            MetodsGeneric.randomWait(15000, 18000);
            Map<String, Object> accounts = bbvaMenuService.extractAccounts(driver);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, accounts.get(Constantes.KEY_DATA));
            result.put(Constantes.KEY_COUNT, accounts.get(Constantes.KEY_COUNT));

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error obteniendo saldo BCP: {}", e.getMessage());

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin, boolean detalle) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 1. ✅ Navegar a la opción "Inicio" del menú lateral
            boolean clickPosicionGlobal = bbvaMenuService.clickCuentas(driver);
            if (!clickPosicionGlobal) {
                throw new BbvaException("No se pudo navegar a cuentas",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            MetodsGeneric.randomWait(8000, 10000);

            boolean clickMovimientos = bbvaMenuService.clickMovimientos(driver);
            if (!clickMovimientos) {
                throw new BbvaException("No se pudo navegar a movimientos",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de movimientos");
            }
            MetodsGeneric.randomWait(5000, 8000);

            // 4. ✅ Aplicar filtros y esperar resultados
            bbvaMenuService.busquedaMovimientos(driver, numeroCuenta, fechaInicio, fechaFin);

            MetodsGeneric.randomWait(5000, 8000);
            // 5. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bbvaMenuService.extraerMovimientos(driver);

            // 6. ✅ Extraer detalle de movimientos si el indicador es true
            if(detalle) {
                movimientos = bbvaMenuService.extraerDetalleMovimientos(driver, movimientos);
            }
            driver.switchTo().defaultContent();

            // 7. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, movimientos);
            result.put(Constantes.KEY_COUNT, movimientos.size());
            result.put("cuenta", numeroCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (BbvaException e){
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        } catch (Exception e) {
            log.error("Error obteniendo movimientos BBVA: {}", e.getMessage());
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientosHistoricos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BCP, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            WebDriver driver = driverCache.get(transactionId);

            if (driver == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }

            // 1. ✅ Navegar a la opción "Cuentas" del menú lateral
            boolean clickPosicionGlobal = bbvaMenuService.clickCuentas(driver);
            if (!clickPosicionGlobal) {
                throw new BbvaException("No se pudo navegar a cuentas",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 2. ✅ Esperar a que cargue la página de resumen
            MetodsGeneric.randomWait(8000, 10000);

            boolean clickMovimientosHistorico = bbvaMenuService.clickMovimientosHistoricos(driver);
            if (!clickMovimientosHistorico) {
                throw new BbvaException("No se pudo navegar a movimientos historicos",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de movimientos historicos");
            }
            MetodsGeneric.randomWait(5000, 8000);

            // 4. ✅ Aplicar filtros y esperar resultados
            bbvaMenuService.busquedaMovimientosHistoricos(driver, numeroCuenta, fechaInicio, fechaFin);

            MetodsGeneric.randomWait(5000, 8000);
            // 5. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bbvaMenuService.extraerMovimientosHistoricos(driver);

            driver.switchTo().defaultContent();

            // 6. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos hisotricos obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, movimientos);
            result.put(Constantes.KEY_COUNT, movimientos.size());
            result.put("cuenta", numeroCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
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

            // 1. ✅ Clic en boton salir
            boolean logoutSuccess = bbvaMenuService.clickSalir(driver);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            }

            // 2. ✅ CERRAR EL DRIVER
            driver.quit();
            log.debug("Driver cerrado exitosamente");
        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());
            driver.quit();
            throw new BbvaException("Error en logout", "BCP_LOGOUT_ERROR", e.getMessage());
        } finally {
            driverCache.remove(transactionId);
            log.info("Sesión {} removida del cache", transactionId);
        }
        return ResponseGeneric.buildSuccessResponse(transactionId, "Sesión cerrada exitosamente", true);
    }



    /**
     * Ingresa el código del usuario
     *
     * @param driver manejador de pagina
     * @param enterpriseCode código de usuario o número de tarjeta
     */
    private void enterEnterpriseCode(WebDriver driver, String enterpriseCode) {
        log.info("Ingresando codigo de empresa...");

        try {
            String code = enterpriseCode.trim();
            final int maxAttempts = 3;
            final By locator = By.xpath("//input[@id='empresa' and @name='cod_emp']");
            WebElement inputLogin = waitForElement(driver, locator);
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(locator));

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                inputLogin.clear();
                MetodsGeneric.humanTypeText(inputLogin, code);

                MetodsGeneric.randomWait(200, 500);

                String actual = inputLogin.getAttribute("value");

                if (actual.equals(code)) {
                    log.info("Código de empresa ingresado correctamente: {}", actual);
                    return;
                } else if (actual.length() == 8 && !actual.equals(code)) {
                    log.warn("El valor tiene 8 dígitos pero no coincide. Esperado={}, Actual={}", code, actual);
                } else {
                    log.warn("Valor incompleto tras intento {}. Esperado 8 dígitos, actual='{}' (len={})", attempt, actual, actual.length());
                }
            }

            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(1000, 3000);

            log.info("Usuario ingresado y tabs aplicados");

        } catch (TimeoutException e) {
            throw BbvaException.elementNotFound("input enterprise", "@id='empresa'");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el código del usuario
     *
     * @param driver manejador de pagina
     * @param userCode código de usuario o número de tarjeta
     */
    private void enterUserCode(WebDriver driver, String userCode) {
        log.info("Ingresando código de usuario...");

        try {
            WebElement inputLogin = waitForElement(driver, By.xpath("//input[@id='usuario' and @name='cod_usu']"));

            inputLogin.clear();
            MetodsGeneric.humanTypeText(inputLogin, userCode);

            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(1000, 3000);

            log.info("Usuario ingresado y tabs aplicados");

        } catch (TimeoutException e) {
            throw BbvaException.elementNotFound("input user", "@id='usuario'");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el código del usuario
     *
     * @param driver manejador de pagina
     * @param password código de usuario o número de tarjeta
     */
    private void enterPassword(WebDriver driver, String password) {
        log.info("Ingresando password...");

        try {
            WebElement inputLogin = waitForElement(driver, By.xpath("//input[@id='clave_acceso_ux' and @name='eai_password']"));

            inputLogin.clear();
            MetodsGeneric.humanTypeText(inputLogin, password);

            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.TAB).perform();
            MetodsGeneric.randomWait(1000, 3000);

            log.info("password ingresado y tabs aplicados");

        } catch (TimeoutException e) {
            throw BbvaException.elementNotFound("input password", "@id='clave_acceso_ux'");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
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
            WebElement loginButton = waitForElement(driver,By.xpath("//button[@id='enviarSenda' and normalize-space()='Ingresar']"));
            loginButton.click();

            log.info("Click en botón de login realizado");

            MetodsGeneric.randomWait(10000, 12000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BbvaException("Interrupción durante la navegación",
                    "BBVA_NAVIGATION_INTERRUPTED",
                    "El proceso fue interrumpido durante la navegación");
        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new BbvaException("Error al hacer click en botón de login",
                    "BBVA_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }

    /**
     * Verifica si hay un modal y lo cierra
     *
     * @param driver manejador de página
     */
    private void closeModalIfPresent(WebDriver driver) throws InterruptedException {
        try {
            WebElement host = driver.findElement(By.cssSelector("bbva-btge-microfrontend-modal[opened]"));

            SearchContext shadow1 = host.getShadowRoot();
            WebElement tmpl = shadow1.findElement(By.cssSelector("bbva-web-template-modal"));
            SearchContext shadow2 = tmpl.getShadowRoot();
            WebElement closeBtn = shadow2.findElement(By.cssSelector("button.close-btn"));
            closeBtn.click();
        } catch (Exception e) {
            log.error("Error al cerrar modal: {}", e.getMessage());
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }
}
