package com.raissa.rpa.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Movimiento {
    @JsonProperty("movimientoUId")
    private String movimientoUId;

    @JsonProperty("fecha")
    private String fecha;

    @JsonProperty("concepto")
    private String concepto;

    @JsonProperty("referencia")
    private String referencia;

    @JsonProperty("debitoCredito")
    private String debitoCredito;

    @JsonProperty("moneda")
    private String moneda;

    @JsonProperty("importe")
    private Double importe;

    @JsonProperty("saldo")
    private String saldo;
}