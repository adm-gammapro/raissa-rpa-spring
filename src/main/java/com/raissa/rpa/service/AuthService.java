package com.raissa.rpa.service;

import com.raissa.rpa.domain.dto.AccountRegistrationRequest;
import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;

import java.util.Map;

public interface AuthService {
    /**
     * Genera token JWT para una cuenta
     *
     * @param account datos de la cuenta
     * @param transactionId id de transaccion
     * @return {@link String} token
     */
    String generateToken(Account account, String transactionId);

    /**
     * Crea una nueva sesión
     *
     * @param account datos de la cuenta
     * @param token token
     * @param transactionId id de transaccion
     * @param clientIp ip del cliente
     * @param userAgent usuario de peticion
     * @return {@link Session} datos de la sesion
     */
    Session createSession(Account account,
                          String token,
                          String transactionId,
                          String clientIp,
                          String userAgent);

    /**
     * Cierra sesión (logout)
     *
     * @param transactionId id de transaccion
     */
    void logout(String transactionId);

    Map<String, Object> decodeToken(String token);

    Account registerAccount(AccountRegistrationRequest request, String createdBy);
}
