package com.raissa.rpa.controller.bcp;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.exception.BcpException;
import com.raissa.rpa.service.bcp.BCPEmpresaService;
import com.raissa.rpa.service.commons.LoggingService;
import com.raissa.rpa.service.commons.ValidationService;
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
@RequestMapping("/api/bcp-empresa")
@RequiredArgsConstructor
@Slf4j
public class BCPEmpresaController {
    private final ValidationService validationService;
    private final BCPEmpresaService bcpEmpresaService;
    private final LoggingService loggingService;

    /**
     * Login en BCP
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

        log.info("Solicitud login BCP recibida");
        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGIN_BCP, userAgent);

        try {
            Map<String, Object> bcpResult = bcpEmpresaService.login(credentials, transactionId);

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
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} datos con cuentas y saldos
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

            Map<String, Object> resp = bcpEmpresaService.obtenerSaldo(transactionId);

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

        log.info("Solicitud transacciones BCP recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_MOV_BCP, userAgent);

        try {
            Map<String, Object> resp = bcpEmpresaService.obtenerMovimientos(transactionId, numCuenta, fechaInicio, fechaFin);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Movimientos BCP obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo movimietnos BCP: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout de BCP
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} respuesta de logout
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
            Map<String, Object> response = bcpEmpresaService.logout(transactionId);

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