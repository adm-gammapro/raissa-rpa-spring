package com.raissa.rpa.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenResponse {
    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("ext_expires_in")
    private Integer extExpiresIn;

    @JsonProperty("access_token")
    private String accessToken;
}
