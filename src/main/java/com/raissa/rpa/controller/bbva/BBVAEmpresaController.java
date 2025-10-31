package com.raissa.rpa.controller.bbva;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.exception.BbvaException;
import com.raissa.rpa.service.bbva.BBVAEmpresaService;
import com.raissa.rpa.service.commons.LoggingService;
import com.raissa.rpa.service.commons.ValidationService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bbva-empresa")
@RequiredArgsConstructor
@Slf4j
public class BBVAEmpresaController {
    private final ValidationService validationService;
    private final BBVAEmpresaService bbvaEmpresaService;
    private final LoggingService loggingService;

    /**
     * Login BBVA
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

        log.info("Solicitud login BBVA recibida");
        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGIN_BBVA, userAgent);

        try {
            Map<String, Object> bbvaResult = bbvaEmpresaService.login(credentials, transactionId);

            if (!(boolean) bbvaResult.get(Constantes.KEY_SUCCESS)) {
                loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);

                throw new BbvaException("Error en login BBVA: " + bbvaResult.get(Constantes.KEY_MESSAGE));
            }

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Login BBVA exitoso para: {}, transactionId: {}", session.getAccount().getFullName(), transactionId);

            return ResponseEntity.ok(bbvaResult);

        } catch (Exception e) {
            log.error("Error en login BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener saldo de BBVA
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} datos con cuentas y saldos
     */
    @PostMapping("/saldo/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerSaldo(@PathVariable String transactionId,
                                                            HttpServletRequest request) {

        log.info("Solicitud saldo BBVA recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_SALDO_BBVA, userAgent);

        try {

            Map<String, Object> resp = bbvaEmpresaService.obtenerSaldo(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Saldo BBVA obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo saldo BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener movimientos del BBVA
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
                                                                    @RequestParam boolean detalle,
                                                                    HttpServletRequest request) {

        log.info("Solicitud transacciones BBVA recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_MOV_BBVA, userAgent);

        try {
            Map<String, Object> resp = bbvaEmpresaService.obtenerMovimientos(transactionId, numCuenta, fechaInicio, fechaFin, detalle);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Movimientos BBVA obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (NoSuchElementException e) {
            log.error("Error obteniendo movimientos BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error  BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Obtener movimientos historicos del BBVA
     *
     * @param transactionId id de transaccion
     * @param numCuenta numero de cuenta
     * @param fechaInicio fecha de inicio para búsqueda
     * @param fechaFin fecha de fin par abúsqueda
     * @param request datos de la peticion
     * @return {@link Map} datos con los movimientos de la cuenta solicitada
     */
    @PostMapping("/transacciones-historicas/{numCuenta}/{transactionId}")
    public ResponseEntity<Map<String, Object>> obtenerTransaccionesHistoricas(@PathVariable String transactionId,
                                                                              @PathVariable String numCuenta,
                                                                              @RequestParam String fechaInicio,
                                                                              @RequestParam String fechaFin,
                                                                              HttpServletRequest request) {

        log.info("Solicitud transacciones BBVA recibida, transactionId: {}", transactionId);

        String clientIp = ResponseGeneric.getClientIp(request);
        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_OBTENER_MOV_BBVA, userAgent);

        try {
            Map<String, Object> resp = bbvaEmpresaService.obtenerMovimientosHistoricos(transactionId, numCuenta, fechaInicio, fechaFin);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Movimientos BBVA obtenido exitosamente, transactionId: {}", transactionId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error obteniendo movimietnos BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Logout BBVA
     *
     * @param transactionId id de transaccion
     * @param request datos de la peticion
     * @return {@link Map} respuesta de logout
     */
    @PostMapping("/logout/{transactionId}")
    public ResponseEntity<Map<String, Object>> logout(@PathVariable String transactionId,
                                                      HttpServletRequest request) {

        log.info("Solicitud logout BBVA recibida, transactionId: {}", transactionId);

        String userAgent = request.getHeader(Constantes.KEY_USER_AGENT);
        Session session = validationService.validateSession(transactionId);
        String clientIp = ResponseGeneric.getClientIp(request);

        RequestInformation logRequest = loggingService.logRequest(session, clientIp, Constantes.TIPO_REQUEST_LOGOUT_BBVA, userAgent);

        try {
            Map<String, Object> response = bbvaEmpresaService.logout(transactionId);

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_EXITO);

            log.info("Logout BBVA exitoso, transactionId: {}", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en logout BBVA: {}", e.getMessage());

            loggingService.updateResponseStatus(logRequest.getId(), Constantes.RESP_REQUEST_ERROR);
            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse(transactionId, e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
