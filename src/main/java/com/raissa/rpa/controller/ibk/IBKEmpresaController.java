package com.raissa.rpa.controller.ibk;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.commons.LoggingService;
import com.raissa.rpa.service.commons.ValidationService;
import com.raissa.rpa.service.ibk.IBKEmpresaService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ibk-empresa")
@RequiredArgsConstructor
@Slf4j
public class IBKEmpresaController {
    private final ValidationService validationService;
    private final IBKEmpresaService ibkEmpresaService;
    private final LoggingService loggingService;

    /**
     * Login en IBK
     *
     * @param credentials datos de acceso a la plataforma
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} respuesta de logueo
     */
    @PostMapping("/login/{transactionId}")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials,
                                                     @PathVariable String transactionId,
                                                     HttpServletRequest request) {

        log.info("Solicitud login IBK recibida");
        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGIN_IBK, userAgent);

        try {
            Map<String, Object> bcpResult = ibkEmpresaService.login(credentials, transactionId);

            if (!(boolean) bcpResult.get(Constantes.KEY_SUCCESS)) {
                loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);

                throw new BcpException("Error en login IBK: " + bcpResult.get(Constantes.KEY_MESSAGE));
            }

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Login IBK exitoso para: {}, transactionId: {}", session.getAccount().getFullName(), transactionId);

            return ResponseEntity.ok(bcpResult);

        } catch (Exception e) {
            log.error("Error en login IBK: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener saldo de IBK
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} datos con cuentas y saldos
     */
    @PostMapping("/saldo/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable String transactionId,
                                                            HttpServletRequest request) {

        log.info("Solicitud saldo IBK recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_SALDO_IBK, userAgent);

        try {

            Map<String, Object> resp = ibkEmpresaService.obtenerSaldo(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Saldo IBK obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo saldo IBK: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener transacciones IBK
     *
     * @param transactionId id de transaccion
     * @param numCuenta numero de cuenta
     * @param fechaInicio fecha de inicio para búsqueda
     * @param fechaFin fecha de fin par abúsqueda
     * @param request datos de la peticion
     * @return {@link Map} datos con los movimientos de la cuenta solicitada
     */
    @PostMapping("/transacciones/{numCuenta}/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerTransacciones(@PathVariable String transactionId,
                                                                    @PathVariable String numCuenta,
                                                                    @RequestParam String fechaInicio,
                                                                    @RequestParam String fechaFin,
                                                                    HttpServletRequest request) {

        log.info("Solicitud transacciones IBK recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_MOV_IBK, userAgent);

        try {
            Map<String, Object> resp = ibkEmpresaService.obtenerMovimientos(transactionId, numCuenta, fechaInicio, fechaFin);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Movimientos IBK obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo movimietnos IBK: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout de IBK
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} respuesta de logout
     */
    @PostMapping("/logout/{transactionId}")
    public ResponseEntity<Map<String, Object>> logout(@PathVariable String transactionId,
                                                      HttpServletRequest request) {

        log.info("Solicitud logout IBK recibida, transactionId: {}", transactionId);

        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);
        String clientIp = ResponseGeneric.getClientIp(request);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGOUT_IBK, userAgent);

        try {
            Map<String, Object> response = ibkEmpresaService.logout(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Logout IBK exitoso, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en logout IBK: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}