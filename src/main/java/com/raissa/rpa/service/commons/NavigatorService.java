package com.raissa.rpa.service.commons;

import com.raissa.rpa.config.NavigatorSession;

public interface NavigatorService {
    NavigatorSession iniciarNavegador(String transactionId);
}
