package com.raissa.rpa.service.impl.commons;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.domain.repository.RequestInformationRepository;
import com.raissa.rpa.service.commons.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingServiceImpl implements LoggingService {
    private final RequestInformationRepository requestInformationRepository;

    @Transactional
    public RequestInformation logRequest(Session session,
                                         String clientIp,
                                         String requestData,
                                         String userAgent) {
        log.info("Registrando request para session: {}", session.getId());

        RequestInformation requestInfo = new RequestInformation();
        requestInfo.setSession(session);
        requestInfo.setConsumerIp(clientIp);
        requestInfo.setRequestData(requestData);
        requestInfo.setCreatedBy(userAgent);
        requestInfo.setActive(1);

        return requestInformationRepository.save(requestInfo);
    }

    @Transactional
    public void updateResponseStatus(Long requestId,
                                     String responseStatus) {
        log.info("Actualizando respuesta para request: {} - Status: {}", requestId, responseStatus);

        requestInformationRepository.findById(requestId).ifPresent(request -> {
            LocalDateTime createdAt = request.getCreatedAt();
            LocalDateTime now = LocalDateTime.now();

            long diferenciaSegundos = Duration.between(createdAt, now).getSeconds();

            request.setResponseStatus(responseStatus);
            request.setActive(0);
            request.setResponseTime(String.valueOf(diferenciaSegundos));
            requestInformationRepository.save(request);
        });
    }

    @Transactional
    public void logCompleteRequest(Session session,
                                   String clientIp,
                                   String requestData,
                                   String responseStatus,
                                   String responseTime,
                                   String userAgent,
                                   Integer activo) {
        RequestInformation requestInfo = new RequestInformation();
        requestInfo.setSession(session);
        requestInfo.setConsumerIp(clientIp);
        requestInfo.setCreatedBy(userAgent);
        requestInfo.setRequestData(requestData);
        requestInfo.setResponseStatus(responseStatus);
        requestInfo.setResponseTime(responseTime);
        requestInfo.setActive(activo);

        requestInformationRepository.save(requestInfo);
    }
}
