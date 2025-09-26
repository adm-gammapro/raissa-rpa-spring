package com.raissa.rpa.service;

import java.util.Map;

public interface BCPService {
    Map<String, Object> login(Map<String, String> credentials);

    Map<String, Object> obtenerSaldo(String transactionId);

    //String obtenerTransacciones(String fechaInicio, String fechaFin);

    void logout();

    String generateTransactionId();
}
