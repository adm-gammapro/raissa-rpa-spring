package com.raissa.rpa.service.impl.bbva;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import com.raissa.rpa.config.NavigatorSession;
import com.raissa.rpa.exception.BbvaException;
import com.raissa.rpa.exception.IbkException;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.bbva.BBVAEmpresaService;
import com.raissa.rpa.service.bbva.BbvaMenuService;
import com.raissa.rpa.service.commons.NavigatorService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.MetodsGeneric;
import com.raissa.rpa.util.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BBVAEmpresaServiceImpl implements BBVAEmpresaService {
    @Value("${banking.bbva.url}")
    private String bbvaUrl;

    private final NavigatorService navigatorService;
    private final BbvaMenuService bbvaMenuService;

    private final ConcurrentMap<String, NavigatorSession> navigatorSessionCache = new ConcurrentHashMap<>();

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login BBVA");

        NavigatorSession sessionNavegacion = null;
        boolean success = false;
        Map<String, Object> result;

        try {
            sessionNavegacion = navigatorService.iniciarNavegador(transactionId);
            Page page = sessionNavegacion.page();

            page.navigate(bbvaUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.LOAD)
                    .setTimeout(30_000));

            log.info("Navegando a: {}", bbvaUrl);

            MetodsGeneric.randomWaitPage(page,800, 1_000);

            // 1. ✅ Ingresar código de empresa
            enterEnterpriseCode(page, credentials.get("codigoEmpresa"));

            // 2. ✅ Ingresar código de usuario
            enterUserCode(page, credentials.get("codigoUsuario"));

            // 3. ✅ INGRESAR CLAVE CON TECLADO VIRTUAL
            enterPassword(page, credentials.get("claveAcceso"));

            // 5. ✅ CLICK EN BOTÓN LOGIN
            clickLoginButton(page);

            // 6. ✅ VERIFICAR si hay modal y cerrarlo
            bbvaMenuService.closeModalIfPresent(page);

            // 7. ✅ VERIFICAR LOGIN EXITOSO
            boolean loginSuccess = bbvaMenuService.isMenuVisible(page);

            if (!loginSuccess) {
                throw new BbvaException("Error en el login después de enviar formulario",
                        "BBVA_LOGIN_VERIFICATION_ERROR",
                        "Error al verificar el login");
            }

            // 8. ✅ ÉXITO - Almacenar driver y retornar resultado
            navigatorSessionCache.put(transactionId, sessionNavegacion);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Login BBVA exitoso", true);

            log.info("Login BBVA completado exitosamente");

            success = true;

            return result;
        } catch (Exception e) {
            log.error("Error genérico en login BBVA: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(transactionId, "Error interno del sistema. Contacte al administrador.", false);
            errorResult.put(Constantes.KEY_ERROR_CODE, "BBVA_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());

            return errorResult;
        } finally {
            if (sessionNavegacion != null && !success) {
                try {
                    sessionNavegacion.close();
                    log.info("Sesión Playwright cerrada debido a error");
                } catch (Exception e) {
                    log.warn("Error al cerrar sesión Playwright: {}", e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> obtenerSaldo(String transactionId) {
        Map<String, Object> result;
        log.info("Obteniendo saldo BBVA, transactionId: {}", transactionId);

        try {
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }
            Page page = session.page();

            boolean clickCuentas = bbvaMenuService.clickCuentas(page);
            if (!clickCuentas) {
                throw new BbvaException("No se pudo navegar a cuentas",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            boolean clickAllCuentas = bbvaMenuService.clickAllCuentas(page);
            if (!clickAllCuentas) {
                throw new BbvaException("No se pudo navegar al detalle de cuentas desde saldos",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de detalle de cuentas");
            }
            Map<String, Object> accounts = bbvaMenuService.extractAccounts(page);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, accounts.get(Constantes.KEY_DATA));
            result.put(Constantes.KEY_COUNT, accounts.get(Constantes.KEY_COUNT));

            return result;
        } catch (BbvaException e){
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        } catch (Exception e) {
            log.error("Error obteniendo saldo BBVA: {}", e.getMessage());

            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> obtenerMovimientos(String transactionId, String numeroCuenta, String fechaInicio, String fechaFin, boolean detalle) {
        Map<String, Object> result;
        log.info("Obteniendo movimientos BBVA, transactionId: {}, cuenta: {}, fechaInicio: {}, fechaFin: {}",
                transactionId, numeroCuenta, fechaInicio, fechaFin);

        try {
            NavigatorSession session = navigatorSessionCache.get(transactionId);
            if (session == null) {
                throw new SessionNotFoundException("Sesión no encontrada");
            }
            Page page = session.page();

            // 1. ✅ Navegar a la opción "Inicio" del menú lateral
            boolean clickCuentas = bbvaMenuService.clickCuentas(page);
            if (!clickCuentas) {
                throw new BbvaException("No se pudo navegar a cuentas",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de cuentas");
            }

            // 2. ✅ Navegar hacia la opcion de todas las cuentas
            boolean clickAllCuentas = bbvaMenuService.clickAllCuentas(page);
            if (!clickAllCuentas) {
                throw new BbvaException("No se pudo navegar al detalle de cuentas desde movimientos",
                        "BBVA_NAVIGATION_ERROR",
                        "No se pudo acceder a la sección de detalle de cuentas");
            }

            // 4. ✅ Aplicar filtros y esperar resultados
            bbvaMenuService.busquedaMovimientos(page, numeroCuenta, fechaInicio, fechaFin);

            // 5. ✅ Extraer datos de movimientos
            List<Map<String, Object>> movimientos = bbvaMenuService.extraerMovimientos(page);

            // 6. ✅ Extraer detalle de movimientos si el indicador es true
            if(detalle) {
                movimientos = bbvaMenuService.extraerDetalleMovimientos(page, movimientos);
            }

            // 7. ✅ Retornar resultados
            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, movimientos);
            result.put(Constantes.KEY_COUNT, movimientos.size());
            result.put("cuenta", numeroCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;

        } catch (BbvaException e){
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        } catch (Exception e) {
            log.error("Error obteniendo movimientos BBVA: {}", e.getMessage());
            return ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);
        }
    }

    public Map<String, Object> logout(String transactionId) {
        NavigatorSession session = navigatorSessionCache.get(transactionId);
        if (session == null) {
            throw new SessionNotFoundException("Sesión no encontrada");
        }
        Page page = session.page();
        try {
            log.info("Iniciando proceso de logout para transactionId: {}", transactionId);

            // 1. ✅ Clic en boton salir
            boolean logoutSuccess = bbvaMenuService.clickSalir(page);

            if (!logoutSuccess) {
                log.warn("No se pudo verificar logout exitoso, cerrando navegador directamente");
            } else {
                bbvaMenuService.clickCerrarSesionModal(page);
            }

            MetodsGeneric.randomWait(1_000, 2_000);
            // 2. ✅ CERRAR EL DRIVER
            page.context().close(); // cierra el contexto de esta sesión
            log.debug("Driver cerrado exitosamente");
        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());
            try {
                page.context().close();
            } catch (Exception ex) {
                log.warn("Error al cerrar contexto: {}", ex.getMessage());
            }
            throw new BbvaException("Error en logout", "BBVA_LOGOUT_ERROR", e.getMessage());
        } finally {
            navigatorSessionCache.remove(transactionId);
            log.info("Sesión {} removida del cache", transactionId);
        }
        return ResponseGeneric.buildSuccessResponse(transactionId, "Sesión cerrada exitosamente", true);
    }



    /**
     * Ingresa el código del usuario
     *
     * @param page manejador de pagina
     * @param enterpriseCode código de usuario o número de tarjeta
     */
    private void enterEnterpriseCode(Page page, String enterpriseCode) {
        log.info("Ingresando codigo de empresa...");

        try {
            String code = enterpriseCode.trim();
            final int maxAttempts = 3;
            final int timeoutMs = 5_000;

            Locator inputLogin = MetodsGeneric.waitForVisible(
                    page,
                    "input#empresa[name='cod_emp']",
                    timeoutMs
            );

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                inputLogin.clear();
                MetodsGeneric.humanTypeText(inputLogin, code, 120, 220);

                MetodsGeneric.randomWaitPage(page, 200, 500);

                String actual = inputLogin.inputValue();

                if (code.equals(actual)) {
                    log.info("Código de empresa ingresado correctamente: {}", actual);
                    return;
                } else if (actual != null && actual.length() == 8 && !actual.equals(code)) {
                    log.warn("El valor tiene 8 dígitos pero no coincide. Esperado={}, Actual={}", code, actual);
                } else {
                    int len = (actual == null) ? 0 : actual.length();
                    log.warn("Valor incompleto tras intento {}. Esperado 8 dígitos, actual='{}' (len={})", attempt, actual, len);
                }
            }

            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page, 500, 1_000);

            log.info("Usuario ingresado y tabs aplicados");

        } catch (TimeoutError e) {
            throw BbvaException.elementNotFound("input enterprise", "@id='empresa'");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el código del usuario
     *
     * @param page manejador de pagina
     * @param userCode código de usuario o número de tarjeta
     */
    private void enterUserCode(Page page, String userCode) {
        log.info("Ingresando código de usuario...");

        try {
            int timeoutMs = 5_000;
            Locator inputLogin = MetodsGeneric.waitForVisible(
                    page,
                    "input#usuario[name='cod_usu']",
                    timeoutMs
            );

            inputLogin.fill("");
            MetodsGeneric.humanTypeText(inputLogin, userCode, 120, 220);

            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page, 500, 1_000);

            log.info("Usuario ingresado y tabs aplicados");

        } catch (TimeoutError e) {
            throw BbvaException.elementNotFound("input user", "@id='usuario'");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
        }
    }

    /**
     * Ingresa el código del usuario
     *
     * @param page manejador de pagina
     * @param password código de usuario o número de tarjeta
     */
    private void enterPassword(Page page, String password) {
        log.info("Ingresando password...");

        try {
            int timeoutMs = 5_000;
            Locator inputLogin = MetodsGeneric.waitForVisible(
                    page,
                    "input#clave_acceso_ux[name='eai_password']",
                    timeoutMs
            );
            inputLogin.fill("");
            MetodsGeneric.humanTypeText(inputLogin, password, 120, 220);

            page.keyboard().press("Tab");
            MetodsGeneric.randomWaitPage(page, 500, 1_000);

            log.info("password ingresado y tabs aplicados");

        } catch (TimeoutError e) {
            throw BbvaException.elementNotFound("input password", "@id='clave_acceso_ux'");
        } catch (Exception e) {
            throw new BbvaException("Error al aplicar tabs de navegación",
                    "BBVA_NAVIGATION_ERROR",
                    "Error en la navegación del portal del banco");
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
            Locator loginButton = MetodsGeneric.waitForVisible(
                    page,
                    "//button[@id='enviarSenda' and normalize-space()='Ingresar']",
                    timeoutMs
            );

            MetodsGeneric.clickWithFallback(page, loginButton, timeoutMs);

            log.info("Click en botón de login realizado");
        } catch (Exception e) {
            log.error("Error haciendo click en botón login: {}", e.getMessage());
            throw new BbvaException("Error al hacer click en botón de login",
                    "BBVA_LOGIN_BUTTON_ERROR",
                    "Error al enviar el formulario de login");
        }
    }
}