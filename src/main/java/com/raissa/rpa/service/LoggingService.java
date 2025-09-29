package com.raissa.rpa.service;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;

public interface LoggingService {
    /**
     * Registra datos de un request
     *
     * @param session datos de la sesion
     * @param clientIp ip de la peticion
     * @param requestData tipo de peticion
     */
    RequestInformation logRequest(Session session,
                                  String clientIp,
                                  String requestData,
                                  String userAgent);

    /**
     * Actualiza el estado de respuesta de un request
     *
     * @param requestId id del request
     * @param responseStatus Resultado de la peticion
     */
    void updateResponseStatus(Long requestId,
                              String responseStatus);

    /**
     * Registra request y respuesta
     *
     * @param session datos de la sesion
     * @param clientIp ip de la peticion
     * @param requestData tipo de peticion
     * @param responseStatus Resultado de la peticion
     * @param responseTime tiempo que duro la peticion
     */
    void logCompleteRequest(Session session,
                            String clientIp,
                            String requestData,
                            String responseStatus,
                            String responseTime,
                            String userAgent,
                            Integer activo);
}
