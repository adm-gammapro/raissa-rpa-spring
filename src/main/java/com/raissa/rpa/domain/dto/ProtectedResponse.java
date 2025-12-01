package com.raissa.rpa.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtectedResponse {
    @JsonProperty("SessionToken")
    private String sessionToken;
}
