package com.raissa.rpa.service.impl;

import com.raissa.rpa.domain.dto.AccountRegistrationRequest;
import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.domain.entity.Session;
import com.raissa.rpa.domain.repository.AccountRepository;
import com.raissa.rpa.domain.repository.SessionRepository;
import com.raissa.rpa.exception.SessionNotFoundException;
import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.service.ValidationService;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final SessionRepository sessionRepository;
    private final AccountRepository accountRepository;
    private final SecretKey secretKey;
    private final ValidationService validation;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(ValidationService validationService,
                           SessionRepository sessionRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.sessionRepository = sessionRepository;
        this.accountRepository = accountRepository;
        this.secretKey = Jwts.SIG.HS256.key().build();
        this.validation = validationService;
        this.passwordEncoder = passwordEncoder;
    }

    public String generateToken(Account account, String transactionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", account.getFullName());
        claims.put("documentType", account.getDocumentType());
        claims.put("documentNumber", account.getDocumentNumber());
        claims.put("transactionId", transactionId);

        return Jwts.builder()
                .claims(claims)
                .subject(account.getDocumentNumber())
                .issuedAt(new Date())
                .expiration(Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(secretKey)
                .compact();
    }

    @Transactional
    public Session createSession(Account account,
                                 String token,
                                 String transactionId,
                                 String clientIp,
                                 String userAgent) {
        log.info("Creando sesión para account: {}, transactionId: {}", account.getId(), transactionId);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        Session session = new Session();
        session.setAccount(account);
        session.setToken(token);
        session.setTransactionId(transactionId);
        session.setExpires(expiresAt);
        session.setConsumerUseragent(userAgent);
        session.setConsumerIp(clientIp);
        return sessionRepository.save(session);
    }

    @Transactional
    public void logout(String transactionId) {
        log.info("Cerrando sesión con transactionId: {}", transactionId);

        Session session = validation.validateSession(transactionId);
        session.setActive(0);
        session.setDisabledAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /**
     * Decodifica token JWT (para validaciones)
     */
    public Map<String, Object> decodeToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new SessionNotFoundException("Token inválido o expirado");
        }
    }

    /**
     * Registrar nueva cuenta de usuario
     */
    @Transactional
    public Account registerAccount(AccountRegistrationRequest request, String createdBy) {
        log.info("Registrando nueva cuenta para: {}", request.getDocumentNumber());

        // Validar que el documento no exista
        Optional<Account> existingAccount = accountRepository.findByDocumentNumberAndActive(request.getDocumentNumber(), 1);
        if (existingAccount.isPresent()) {
            throw new RuntimeException("Ya existe una cuenta con este número de documento");
        }

        // Validar que el key_access no exista
        boolean existingKeyAccess = accountRepository.existsByKeyAccess(request.getKeyAccess());
        if (existingKeyAccess) {
            throw new RuntimeException("El key_access ya está en uso");
        }

        // Crear nueva cuenta
        Account account = new Account();
        account.setFullName(request.getFullName());
        account.setDocumentType(request.getDocumentType());
        account.setDocumentNumber(request.getDocumentNumber());
        account.setKeyAccess(request.getKeyAccess());

        // Encriptar secret_access
        account.setSecretAccess(passwordEncoder.encode(request.getSecretAccess()));

        account.setActive(1);
        account.setCreatedAt(LocalDateTime.now());
        account.setCreatedBy(createdBy);
        account.setUpdatedAt(LocalDateTime.now());
        account.setUpdatedBy(createdBy);

        Account savedAccount = accountRepository.save(account);

        log.info("Cuenta registrada exitosamente - ID: {}, Documento: {}",
                savedAccount.getId(), savedAccount.getDocumentNumber());

        return savedAccount;
    }
}
