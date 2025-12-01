package com.raissa.rpa.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaldoResponse {
    @JsonProperty("Moneda")
    private String moneda;

    @JsonProperty("Disponible")
    private Double disponible;

    @JsonProperty("SaldoContable")
    private Double saldoContable;
}
