package com.raissa.rpa.service;

import com.raissa.rpa.domain.entity.RequestInformation;
import com.raissa.rpa.domain.entity.Session;

public interface LoggingService {
    RequestInformation logRequest(Session session, String clientIp, String requestData);

    void updateResponseStatus(Long requestId, String responseStatus, String responseTime);

    RequestInformation logCompleteRequest(Session session, String clientIp,
                                                 String requestData, String responseStatus,
                                                 String responseTime);
}
