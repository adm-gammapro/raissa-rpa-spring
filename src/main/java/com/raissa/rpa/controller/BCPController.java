package com.raissa.rpa.controller;

import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.service.BCPService;
import com.raissa.rpa.service.LoggingService;
import com.raissa.rpa.service.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bcp")
@RequiredArgsConstructor
@Slf4j
public class BCPController {
    private final ValidationService validationService;
    private final AuthService authService;
    private final BCPService bcpService;
    private final LoggingService loggingService;

    /**
     * Login en BCP
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials,
                                                     HttpServletRequest request) {

        log.info("Solicitud login BCP recibida");

        try {
            Map<String, Object> bcpResult = bcpService.login(credentials);

            if (!(Boolean) bcpResult.get("success")) {
                throw new RuntimeException("Error en login BCP: " + bcpResult.get("message"));
            }

            /*loggingService.logRequest(session, clientIp, "LOGIN_BCP");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("token", token);
            response.put("transaction_id", transactionId);
            response.put("full_name", account.getFullName());
            response.put("bcp_session_id", bcpResult.get("transactionId"));

            log.info("Login BCP exitoso para: {}, transactionId: {}", account.getFullName(), transactionId);*/

            return ResponseEntity.ok(bcpResult);

        } catch (Exception e) {
            log.error("Error en login BCP: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener saldo de BCP
     */
    @GetMapping("/saldo")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String transactionId,
            HttpServletRequest request) {

        log.info("Solicitud saldo BCP recibida, transactionId: {}", transactionId);

        try {
            // Validar sesión
            Session session = validationService.validateSession(transactionId);

            // Obtener saldo
            Map<String, Object> resp = bcpService.obtenerSaldo("dddddddd");

            // Loggear request
            String clientIp = getClientIp(request);
            loggingService.logCompleteRequest(session, clientIp, "OBTENER_SALDO","EXITO", "Tiempo estimado");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("saldo", resp.get("saldo"));
            response.put("transaction_id", transactionId);

            log.info("Saldo BCP obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo saldo BCP: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener transacciones BCP
     */
    @GetMapping("/transacciones")
    public ResponseEntity<Map<String, Object>> obtenerTransacciones(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String transactionId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            HttpServletRequest request) {

        log.info("Solicitud transacciones BCP recibida, transactionId: {}", transactionId);

        try {
            // Validar sesión
            Session session = validationService.validateSession(transactionId);

            // Obtener transacciones
            //String transacciones = bcpService.obtenerTransacciones(fechaInicio, fechaFin);

            // Loggear request
            String clientIp = getClientIp(request);
            String requestData = String.format("FECHAS=%s-%s", fechaInicio, fechaFin);
            loggingService.logCompleteRequest(session, clientIp, requestData,
                    "EXITO", "Tiempo estimado");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transacciones", "transacciones");
            response.put("transaction_id", transactionId);
            response.put("fecha_inicio", fechaInicio);
            response.put("fecha_fin", fechaFin);

            log.info("Transacciones BCP obtenidas exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo transacciones BCP: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout de BCP
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestParam String transactionId,
            HttpServletRequest request) {

        log.info("Solicitud logout BCP recibida, transactionId: {}", transactionId);

        try {
            // Validar y cerrar sesión
            Session session = validationService.validateSession(transactionId);
            authService.logout(transactionId);

            // Cerrar sesión en BCP
            bcpService.logout();

            // Loggear request
            String clientIp = getClientIp(request);
            loggingService.logCompleteRequest(session, clientIp, "LOGOUT",
                    "EXITO", "Tiempo estimado");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout exitoso");
            response.put("transaction_id", transactionId);

            log.info("Logout BCP exitoso, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en logout BCP: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener IP del cliente
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}