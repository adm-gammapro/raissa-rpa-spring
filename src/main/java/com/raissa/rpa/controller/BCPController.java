package com.raissa.rpa.controller;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.service.BCPService;
import com.raissa.rpa.service.LoggingService;
import com.raissa.rpa.service.ValidationService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/login/{transactionId}")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials,
                                                                 @PathVariable String transactionId,
                                                                 HttpServletRequest request) {

        log.info("Solicitud login BCP recibida");
        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGIN_BCP, userAgent);

        try {
            Map<String, Object> bcpResult = bcpService.login(credentials, transactionId);

            if (!(boolean) bcpResult.get(Constantes.KEY_SUCCESS)) {
                loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);

                throw new BcpException("Error en login BCP: " + bcpResult.get(Constantes.KEY_MESSAGE));
            }

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Login BCP exitoso para: {}, transactionId: {}", session.getAccount().getFullName(), transactionId);

            return ResponseEntity.ok(bcpResult);

        } catch (Exception e) {
            log.error("Error en login BCP: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener saldo de BCP
     */
    @PostMapping("/saldo/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable String transactionId,
                                                            HttpServletRequest request) {

        log.info("Solicitud saldo BCP recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_SALDO_BCP, userAgent);

        try {

            Map<String, Object> resp = bcpService.obtenerSaldo(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Saldo BCP obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo saldo BCP: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener transacciones BCP
     */
    @GetMapping("/transacciones/{numCuenta}/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerTransacciones(@PathVariable String transactionId,
                                                                    @PathVariable String numCuenta,
                                                                    @RequestParam String fechaInicio,
                                                                    @RequestParam String fechaFin,
                                                                    HttpServletRequest request) {

        log.info("Solicitud transacciones BCP recibida, transactionId: {}", transactionId);

        try {
            // Validar sesión
            Session session = validationService.validateSession(transactionId);

            // Obtener transacciones
            //String transacciones = bcpService.obtenerTransacciones(fechaInicio, fechaFin);

            String clientIp = ResponseGeneric.getClientIp(request);
            String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);

            String requestData = String.format("FECHAS=%s-%s", fechaInicio, fechaFin);
            loggingService.logCompleteRequest(session, clientIp, requestData,Constantes.RESP_REQUEST_EXITO, "0",userAgent, 0);

            Map<String, Object> response = ResponseGeneric.buildSuccessResponse(transactionId, "Se obtiene movimientos correctamente", true);
            response.put("transacciones", "transacciones");
            response.put("fecha_inicio", fechaInicio);
            response.put("fecha_fin", fechaFin);

            log.info("Transacciones BCP obtenidas exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo transacciones BCP: {}", e.getMessage());

            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout de BCP
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map}
     */
    @PostMapping("/logout/{transactionId}")
    public ResponseEntity<Map<String, Object>> logout(@PathVariable String transactionId,
                                                      HttpServletRequest request) {

        log.info("Solicitud logout BCP recibida, transactionId: {}", transactionId);

        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);
        String clientIp = ResponseGeneric.getClientIp(request);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGOUT_BCP, userAgent);

        try {
            Map<String, Object> response = bcpService.logout(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Logout BCP exitoso, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en logout BCP: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}