package com.raissa.rpa.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovimientosResponse {
    @JsonProperty("sdtEstadoDeCuenta")
    private EstadoCuenta estadoDeCuenta;

    public List<Movimiento> getMovimientos() {
        if (estadoDeCuenta == null || estadoDeCuenta.movimientos == null) {
            return Collections.emptyList();
        }
        return estadoDeCuenta.movimientos.movimientos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EstadoCuenta {
        @JsonProperty("movimientos")
        private Movimientos movimientos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class Movimientos {
        @JsonProperty("sBTMovimiento")
        private List<Movimiento> movimientos = new ArrayList<>();
    }
}
