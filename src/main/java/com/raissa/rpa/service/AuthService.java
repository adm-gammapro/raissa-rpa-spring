package com.raissa.rpa.service;

import com.raissa.rpa.domain.dto.AccountRegistrationRequest;
import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;

import java.util.Map;

public interface AuthService {
    String generateToken(Account account, String transactionId);

    Session createSession(Account account,
                                 String token,
                                 String transactionId,
                                 String clientIp,
                                 String userAgent);

    void logout(String transactionId);

    Map<String, Object> decodeToken(String token);

    Account registerAccount(AccountRegistrationRequest request, String createdBy);
}
