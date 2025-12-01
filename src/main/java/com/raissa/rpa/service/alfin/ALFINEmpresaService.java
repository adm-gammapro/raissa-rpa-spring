package com.raissa.rpa.service.alfin;

import java.util.Map;

public interface ALFINEmpresaService {
    Map<String, Object> login(Map<String, String> credentials,
                              String transactionId);

    Map<String, Object> saldos(Map<String, String> datos,
                               String transactionId);

    Map<String, Object> movimientos(String tokenAlterno,
                                    String sessionToken,
                                    String transactionId,
                                    String usuario,
                                    String numCuenta,
                                    String fechaInicio,
                                    String fechaFin);
}
