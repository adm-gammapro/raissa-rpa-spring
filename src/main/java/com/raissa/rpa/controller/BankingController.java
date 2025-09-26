package com.raissa.rpa.controller;

import com.raissa.rpa.domain.dto.AccountRegistrationRequest;
import com.raissa.rpa.domain.entity.Account;
import com.raissa.rpa.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@Slf4j
@RequiredArgsConstructor
public class BankingController {
    private final AuthService authService;

    /**
     * 📁 Registrar nueva cuenta de usuario
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAccount(@RequestBody AccountRegistrationRequest request,
                                                               HttpServletRequest httpRequest) {

        log.info("Solicitud de registro recibida para documento: {}", request.getDocumentNumber());

        try {
            String createdBy = "system";

            // Registrar la cuenta
            Account account = authService.registerAccount(request, createdBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cuenta registrada exitosamente");
            response.put("account_id", account.getId());
            response.put("full_name", account.getFullName());
            response.put("document_number", account.getDocumentNumber());
            response.put("key_access", account.getKeyAccess());

            log.info("Registro exitoso - Cuenta ID: {}, Documento: {}", account.getId(), account.getDocumentNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en registro de cuenta: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
