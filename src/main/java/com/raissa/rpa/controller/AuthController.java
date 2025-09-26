package com.raissa.rpa.controller;

import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.service.LoggingService;
import com.raissa.rpa.service.ValidationService;
import com.raissa.rpa.util.Constantes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final ValidationService validationService;
    private final AuthService authService;
    private final LoggingService loggingService;

    private final Random random = new Random();

    /**
     * Login - Autenticación y generación de token
     * @param credentials datos de credenciales
     * @param request datos de peticion
     * return {@link Map} creo una nueva sesion y un token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials,
                                                     HttpServletRequest request) {

        log.info("Solicitud de autenticación recibida");

        try {
            String keyAccess = credentials.get("key_access");
            String secretAccess = credentials.get("secret_access");

            Account account = validationService.validateCredentials(keyAccess, secretAccess);

            String transactionId = "AUTH_" + System.currentTimeMillis() + "_" + random.nextInt(1000);

            String token = authService.generateToken(account, transactionId);

            String clientIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            Session session = authService.createSession(account, token, transactionId, clientIp, userAgent);

            loggingService.logRequest(session, clientIp, "LOGIN");

            Map<String, Object> response = new HashMap<>();
            response.put(Constantes.KEY_SUCCESS, true);
            response.put(Constantes.KEY_MESSAGE, "Autenticación exitosa");
            response.put(Constantes.KEY_TOKEN, token);
            response.put(Constantes.KEY_TRANSACTION_ID, transactionId);
            response.put(Constantes.KEY_FUL_NAME, account.getFullName());
            response.put(Constantes.KEY_DOCUMENT_NUMBER, account.getDocumentNumber());
            response.put(Constantes.KEY_EXPIRES_IN, 3600);

            log.info("Autenticación exitosa para: {}, transactionId: {}", account.getFullName(), transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en autenticación: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constantes.KEY_SUCCESS, false);
            errorResponse.put(Constantes.KEY_MESSAGE, e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout - Cierre de sesión
     * @param transactionId id de transaccion
     * @param request datos de peticion
     * return {@link Map} confirma el cierre de sesion
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestParam String transactionId,
                                                      HttpServletRequest request) {

        log.info("Solicitud de logout recibida, transactionId: {}", transactionId);

        try {
            Session session = validationService.validateSession(transactionId);
            authService.logout(transactionId);

            String clientIp = getClientIp(request);
            loggingService.logCompleteRequest(session, clientIp, "LOGOUT", "EXITO", "Tiempo estimado");

            Map<String, Object> response = new HashMap<>();
            response.put(Constantes.KEY_SUCCESS, true);
            response.put(Constantes.KEY_MESSAGE, "Sesión cerrada exitosamente");
            response.put(Constantes.KEY_TRANSACTION_ID, transactionId);

            log.info("Logout exitoso, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en logout: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constantes.KEY_SUCCESS, false);
            errorResponse.put(Constantes.KEY_MESSAGE, e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Validar token - Verificar si un token es válido
     * @param token token generado
     * @param request datos de peticion
     * return {@link Map} datos del token y autorizador
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token,
                                                             HttpServletRequest request) {

        log.info("Solicitud de validación de token recibida");

        try {
            Map<String, Object> tokenData = authService.decodeToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put(Constantes.KEY_SUCCESS, true);
            response.put(Constantes.KEY_MESSAGE, "Token válido");
            response.put(Constantes.KEY_VALID, true);
            response.put(Constantes.KEY_FUL_NAME, tokenData.get("fullName"));
            response.put(Constantes.KEY_DOCUMENT_NUMBER, tokenData.get("documentNumber"));
            response.put(Constantes.KEY_TRANSACTION_ID, tokenData.get("transactionId"));

            log.info("Token validado exitosamente para: {}", tokenData.get("fullName"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constantes.KEY_SUCCESS, false);
            errorResponse.put(Constantes.KEY_MESSAGE, "Token invalido o expirado");
            errorResponse.put(Constantes.KEY_VALID, false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener información de sesión
     *
     * @param transactionId id de transaccion
     * @param request datos de peticion
     * return {@link Map} datos de la sesion
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSessionInfo(@RequestParam String transactionId,
                                                              HttpServletRequest request) {

        log.info("Solicitud de información de sesión, transactionId: {}", transactionId);

        try {
            Session session = validationService.validateSession(transactionId);

            Map<String, Object> response = new HashMap<>();
            response.put(Constantes.KEY_SUCCESS, true);
            response.put(Constantes.KEY_TRANSACTION_ID, session.getTransactionId());
            response.put("created_at", session.getCreatedAt());
            response.put(Constantes.KEY_EXPIRES_IN, session.getExpires());
            response.put("active", session.isActive());
            response.put("client_ip", session.getConsumerIp());
            response.put("user_agent", session.getConsumerUseragent());
            response.put("account_id", session.getAccount().getId());
            response.put(Constantes.KEY_FUL_NAME, session.getAccount().getFullName());

            log.info("Información de sesión obtenida, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo información de sesión: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(Constantes.KEY_SUCCESS, false);
            errorResponse.put(Constantes.KEY_MESSAGE, e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener IP del cliente
     *
     * @param request datos de la peticion
     * return {link String}
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || Constantes.KEY_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || Constantes.KEY_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || Constantes.KEY_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}