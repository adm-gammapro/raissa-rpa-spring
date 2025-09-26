package com.raissa.rpa.service.impl;

import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.domain.repository.AccountRepository;
import com.raissa.rpa.domain.repository.SessionRepository;
import com.raissa.rpa.exception.AccountInactiveException;
import com.raissa.rpa.exception.AccountNotFoundException;
import com.raissa.rpa.exception.AuthenticationException;
import com.raissa.rpa.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account validateCredentials(String keyAccess, String secretAccess) {
        log.info("Validando credenciales para keyAccess: {}", keyAccess);

        Optional<Account> accountOpt = accountRepository.findByKeyAccess(keyAccess);

        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException("Credenciales inválidas");
        }

        Account account = accountOpt.get();

        if (account.getActive() != 1) {
            throw new AccountInactiveException("Cuenta inactiva");
        }

        if (!passwordEncoder.matches(secretAccess, account.getSecretAccess())) {
            throw new AuthenticationException("Credenciales invalidas");
        }

        log.info("Credenciales validadas exitosamente para: {}", account.getFullName());
        return account;
    }

    @Transactional(readOnly = true)
    public Session validateSession(String transactionId) {
        log.info("Validando sesion con transactionId: {}", transactionId);

        return sessionRepository.findByTransactionId(transactionId)
                .filter(Session::isActive)
                .orElseThrow(() -> new RuntimeException("Sesion no encontrada o expirada"));
    }
}
