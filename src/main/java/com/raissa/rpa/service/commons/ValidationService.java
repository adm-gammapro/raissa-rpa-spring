package com.raissa.rpa.service.commons;

import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;

public interface ValidationService {
    /**
     * Valida credenciales de acceso (key_access y secret_access)
     *
     * @param keyAccess llave o usuario de acceso
     * @param secretAccess contraseña sin encriptar de la llave o usuario
     * return {@link Account}
     */
    Account validateCredentials(String keyAccess, String secretAccess);

    /**
     * Valida y obtiene una sesión activa por transactionId
     *
     * @param transactionId id de la transaccion
     * return {@link Session}
     */
    Session validateSession(String transactionId);
}
