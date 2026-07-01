package com.raissa.rpa.service.commons;

import com.raissa.rpa.config.NavigatorSession;

public interface NavigatorService {
    NavigatorSession iniciarNavegador(String transactionId, String bankCode);

    void liberarSesion(String transactionId, String bankCode, NavigatorSession session);
}
