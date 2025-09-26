package com.raissa.rpa.domain.dto;

import lombok.Data;

@Data
public class AccountRegistrationRequest {
    private String fullName;
    private String documentType;
    private String documentNumber;
    private String keyAccess;
    private String secretAccess;
}
