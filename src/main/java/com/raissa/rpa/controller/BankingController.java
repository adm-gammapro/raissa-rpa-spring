package com.raissa.rpa.controller;

import com.raissa.rpa.domain.dto.AccountRegistrationRequest;
import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.service.AuthService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@Slf4j
@RequiredArgsConstructor
public class BankingController {
    private final AuthService authService;

    /**
     * Registrar nueva cuenta de usuario
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAccount(@RequestBody AccountRegistrationRequest accountRequest) {

        log.info("Solicitud de registro recibida para documento: {}", accountRequest.getDocumentNumber());

        try {
            String createdBy = "system";

            Account account = authService.registerAccount(accountRequest, createdBy);

            Map<String, Object> response = ResponseGeneric.buildSuccessResponse("-", "Cuenta registrada exitosamente", true);
            response.put("account_id", account.getId());
            response.put(Constantes.KEY_FUL_NAME, account.getFullName());
            response.put(Constantes.KEY_DOCUMENT_NUMBER, account.getDocumentNumber());
            response.put("key_access", account.getKeyAccess());

            log.info("Registro exitoso - Cuenta ID: {}, Documento: {}", account.getId(), account.getDocumentNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en registro de cuenta: {}", e.getMessage());

            Map<String, Object> errorResponse = ResponseGeneric.buildSuccessResponse("-", e.getMessage(), false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}