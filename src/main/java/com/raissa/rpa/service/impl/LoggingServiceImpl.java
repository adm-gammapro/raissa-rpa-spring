package com.raissa.rpa.service.impl;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.domain.repository.RequestInformationRepository;
import com.raissa.rpa.service.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingServiceImpl implements LoggingService {
    private final RequestInformationRepository requestInformationRepository;

    /**
     * Registra un request en la base de datos
     */
    @Transactional
    public RequestInformation logRequest(Session session, String clientIp, String requestData) {
        log.info("Registrando request para session: {}", session.getId());

        RequestInformation requestInfo = new RequestInformation();
        requestInfo.setSession(session);
        requestInfo.setConsumerIp(clientIp);
        requestInfo.setRequestData(requestData);
        return requestInformationRepository.save(requestInfo);
    }

    /**
     * Actualiza el estado de respuesta de un request
     */
    @Transactional
    public void updateResponseStatus(Long requestId, String responseStatus, String responseTime) {
        log.info("Actualizando respuesta para request: {} - Status: {}", requestId, responseStatus);

        requestInformationRepository.findById(requestId).ifPresent(request -> {
            request.setResponseStatus(responseStatus);
            request.setResponseTime(responseTime);
            requestInformationRepository.save(request);
        });
    }

    /**
     * Registra request y respuesta completo
     */
    @Transactional
    public RequestInformation logCompleteRequest(Session session, String clientIp,
                                                 String requestData, String responseStatus,
                                                 String responseTime) {
        RequestInformation requestInfo = logRequest(session, clientIp, requestData);
        requestInfo.setResponseStatus(responseStatus);
        requestInfo.setResponseTime(responseTime);
        return requestInformationRepository.save(requestInfo);
    }
}
