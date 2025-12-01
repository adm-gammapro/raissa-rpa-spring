package com.raissa.rpa.service.impl.alfin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raissa.rpa.domain.dto.Movimiento;
import com.raissa.rpa.domain.dto.MovimientosResponse;
import com.raissa.rpa.domain.dto.ProtectedResponse;
import com.raissa.rpa.domain.dto.SaldoResponse;
import com.raissa.rpa.domain.dto.TokenResponse;
import com.raissa.rpa.domain.entity.Params;
import com.raissa.rpa.domain.repository.ParamsRepository;
import com.raissa.rpa.exception.AlfinException;
import com.raissa.rpa.exception.EmptyResponseException;
import com.raissa.rpa.service.alfin.ALFINEmpresaService;
import com.raissa.rpa.util.Constantes;
import com.raissa.rpa.util.ResponseGeneric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ALFINEmpresaServiceImpl implements ALFINEmpresaService {
    private final ParamsRepository paramsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${banking.alfinapi.url}")
    private String alfinBaseUrl;

    @Value("${banking.alfintoken.url}")
    private String tokenAlfinUrl;

    private static final DateTimeFormatter FORMATO_ENTRADA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_SALIDA  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Map<String, Object> login(Map<String, String> credentials,
                                     String transactionId) {
        log.info("Iniciando proceso de login ALFIN");

        try {
            TokenResult tokenResult = obtenerTokenAlfin();

            log.info("Alfin token validado {}, ------------------------------------------------------------------------------", tokenResult);

            if (!tokenResult.success()) {
                Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(
                        transactionId,
                        tokenResult.userMessage(),
                        false
                );
                errorResult.put(Constantes.KEY_ERROR_CODE, tokenResult.errorCode());
                errorResult.put(Constantes.KEY_TEC_MESSAGE, tokenResult.errorMessage());
                return errorResult;
            }

            String sesionToken = invocarAuthorizationAlfin(tokenResult.token(), credentials);

            Map<String, Object> result = ResponseGeneric.buildSuccessResponse(transactionId, "Login ALFIN exitoso", true);
            result.put("SessionToken", sesionToken);
            result.put("TokenAlterno", tokenResult.token());
            log.info("Login ALFIN completado exitosamente");
            return result;

        } catch (Exception e) {
            log.error("Error genérico en login ALFIN: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(
                    transactionId,
                    "Error interno del sistema. Contacte al administrador.",
                    false
            );
            errorResult.put(Constantes.KEY_ERROR_CODE, "ALFIN_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());
            return errorResult;
        }
    }

    public Map<String, Object> saldos(Map<String, String> datos,
                                      String transactionId) {
        Map<String, Object> result;
        log.info("Iniciando proceso de extraccion de saldos de ALFIN");

        try {

            Map<String, Object> accounts = invocarSaldosAlfin(datos.get("tokenAlterno"), datos.get("sessionToken"), datos);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Datos de cuentas obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, accounts.get(Constantes.KEY_DATA));
            result.put(Constantes.KEY_COUNT, accounts.get(Constantes.KEY_COUNT));

            return result;
        } catch (Exception e) {
            log.error("Error genérico en la obtención de saldos de ALFIN: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(
                    transactionId,
                    "Error interno del sistema. Contacte al administrador.",
                    false
            );
            errorResult.put(Constantes.KEY_ERROR_CODE, "ALFIN_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());
            return errorResult;
        }
    }

    public Map<String, Object> movimientos(String tokenAlterno,
                                           String sessionToken,
                                           String transactionId,
                                           String usuario,
                                           String numCuenta,
                                           String fechaInicio,
                                           String fechaFin) {
        Map<String, Object> result;
        log.info("Iniciando proceso de extraccion de movimientos de ALFIN");

        try {

            List<Map<String, Object>> movimientos = invocarMovimientosAlfin(tokenAlterno, sessionToken, usuario, numCuenta, fechaInicio, fechaFin);

            result = ResponseGeneric.buildSuccessResponse(transactionId, "Movimientos obtenidos exitosamente", true);
            result.put(Constantes.KEY_DATA, movimientos);
            result.put(Constantes.KEY_COUNT, movimientos.size());
            result.put("cuenta", numCuenta);
            result.put("fechaInicio", fechaInicio);
            result.put("fechaFin", fechaFin);

            return result;
        } catch (Exception e) {
            log.error("Error genérico en la obtención de saldos de ALFIN: {}", e.getMessage());

            Map<String, Object> errorResult = ResponseGeneric.buildSuccessResponse(
                    transactionId,
                    "Error interno del sistema. Contacte al administrador.",
                    false
            );
            errorResult.put(Constantes.KEY_ERROR_CODE, "ALFIN_GENERIC_ERROR");
            errorResult.put(Constantes.KEY_TEC_MESSAGE, e.getMessage());
            return errorResult;
        }
    }

    private TokenResult obtenerTokenAlfin() {
        List<Params> paramsList = paramsRepository.findByProviderAndGrupo(
                Constantes.PROVIDER_ALFIN,
                Constantes.GRUPO_LOGIN
        );

        if (paramsList.isEmpty()) {
            String message = "Parámetros de login no configurados para el provider ALFIN";
            log.error(message);
            return TokenResult.failure("ALFIN_PARAMS_NOT_FOUND", message, message);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        paramsList.forEach(param -> formData.add(param.getCodigo(), param.getValor()));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<TokenResponse> response =
                    restTemplate.postForEntity(tokenAlfinUrl, request, TokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                String message = "Respuesta no exitosa del servicio de token ALFIN";
                log.error("{} - status: {}", message, response.getStatusCode());
                return TokenResult.failure("ALFIN_NON_2XX", message, message);
            }

            TokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                String message = "Respuesta vacía o incompleta del servicio de token";
                log.error(message);
                return TokenResult.failure("ALFIN_EMPTY_RESPONSE", message, message);
            }

            return TokenResult.success(body.getAccessToken());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor de seguridad: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AlfinException("Error del servidor de seguridad: " + e.getStatusCode());
        } catch (EmptyResponseException | AlfinException e) {
            log.error("Error de negocio al obtener token ALFIN: {}", e.getMessage());
            return TokenResult.failure("ALFIN_BUSINESS_ERROR", e.getMessage(), e.getMessage());
        } catch (Exception e) {
            String userMessage = "Error inesperado al obtener el token del proveedor ALFIN";
            log.error("{}: {}", userMessage, e.getMessage());
            return TokenResult.failure("ALFIN_UNEXPECTED_ERROR", userMessage, e.getMessage());
        }
    }

    private String invocarAuthorizationAlfin(String accessToken,
                                             Map<String, String> credentials) {
        List<Params> paramsList = paramsRepository.findByProviderAndGrupo(
                Constantes.PROVIDER_ALFIN,
                Constantes.GRUPO_CONSULTAS
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String usuario = credentials.get("codigoUsuario");
        if (StringUtils.hasText(usuario)) {
            headers.set("Usuario", usuario);
        }

        paramsList.stream()
                .filter(param -> !"alfinhash".equalsIgnoreCase(param.getCodigo()))
                .forEach(param -> headers.set(param.getCodigo(), param.getValor()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Userid", credentials.getOrDefault("codigoUsuario", "USGAMMADEV"));
        payload.put("Userpassword", credentials.getOrDefault("claveAcceso", "2ueqGLCYcF@LKD"));

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AlfinException("No se pudo serializar el payload para firmar alfinhash");
        }

        String alfinHashSecret = paramsList.stream()
                .filter(param -> "alfinhash".equalsIgnoreCase(param.getCodigo()))
                .map(Params::getValor)
                .findFirst()
                .orElseThrow(() -> new AlfinException("Clave 'alfinhash' no configurada en parámetros ALFIN"));

        // Firmar payload
        String alfinHash = hmacSha256Hex(payloadJson, alfinHashSecret);
        headers.set("alfinhash", alfinHash);

        headers.set("token", "");

        String url = String.format("%s/api/Authenticate/v1/Execute", alfinBaseUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<ProtectedResponse> response =
                    restTemplate.postForEntity(url, request, ProtectedResponse.class);

            ProtectedResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.getSessionToken())) {
                throw new AlfinException("Autenticación ALFIN sin SessionToken en la respuesta");
            }

            log.debug("Respuesta servicio protegido ALFIN: {}", body);

            return body.getSessionToken();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al invocar servicio protegido ALFIN {}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AlfinException("Error al invocar servicio protegido ALFIN: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error inesperado al invocar servicio protegido ALFIN: {}", e.getMessage());
            throw new AlfinException("Error inesperado al invocar servicio protegido ALFIN: " + e.getMessage());
        }
    }

    private Map<String, Object> invocarSaldosAlfin(String tokenAlterno, String sessionToken, Map<String, String> datos) {
        List<Params> paramsList = paramsRepository.findByProviderAndGrupo(
                Constantes.PROVIDER_ALFIN,
                Constantes.GRUPO_CONSULTAS
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAlterno);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String usuario = datos.get("codigoUsuario");
        if (StringUtils.hasText(usuario)) {
            headers.set("Usuario", usuario);
        }

        headers.set("token", sessionToken);

        paramsList.stream()
                .filter(param -> !"alfinhash".equalsIgnoreCase(param.getCodigo()))
                .forEach(param -> headers.set(param.getCodigo(), param.getValor()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ClienteBaaS", datos.getOrDefault("codigoUsuario", "USGAMMADEV"));
        payload.put("CuentaBaaS", datos.getOrDefault("numeroCuenta", "00012948000001"));

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AlfinException("No se pudo serializar el payload para firmar alfinhash");
        }

        String alfinHashSecret = paramsList.stream()
                .filter(param -> "alfinhash".equalsIgnoreCase(param.getCodigo()))
                .map(Params::getValor)
                .findFirst()
                .orElseThrow(() -> new AlfinException("Clave 'alfinhash' no configurada en parámetros ALFIN"));

        String alfinHash = hmacSha256Hex(payloadJson, alfinHashSecret);
        headers.set("alfinhash", alfinHash);

        String url = String.format("%s/api/ABServices/v1/ObtenerSaldoDisponibleBaaS", alfinBaseUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<SaldoResponse> response =
                    restTemplate.postForEntity(url, request, SaldoResponse.class);

            SaldoResponse body = response.getBody();
            if (body == null) {
                throw new AlfinException("Respuesta vacía del servicio de saldos ALFIN");
            }

            Double saldo = body.getDisponible() != null ? body.getDisponible() : 0.0;
            String moneda = normalizarMoneda(body.getMoneda());

            Map<String, Object> cuentaMap = new HashMap<>();
            cuentaMap.put(Constantes.KEY_NUMERO_CUENTA, datos.getOrDefault("numeroCuenta", null));
            cuentaMap.put(Constantes.KEY_TIPO_CUENTA, "Corriente");
            cuentaMap.put(Constantes.KEY_SALDO_DISP, saldo);
            cuentaMap.put(Constantes.KEY_SALDO_CONT, saldo);
            cuentaMap.put(Constantes.KEY_MONEDA, moneda);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("data", Collections.singletonList(cuentaMap));
            resultado.put("count", 1);

            log.debug("Respuesta saldo ALFIN: {}", resultado);
            return resultado;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al invocar servicio protegido ALFIN {}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AlfinException("Error al invocar servicio protegido ALFIN: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error inesperado al invocar servicio protegido ALFIN: {}", e.getMessage());
            throw new AlfinException("Error inesperado al invocar servicio protegido ALFIN: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> invocarMovimientosAlfin(String tokenAlterno,
                                                              String sessionToken,
                                                              String usuario,
                                                              String numeroCuenta,
                                                              String fechaInicio,
                                                              String fechaFin) {
        List<Params> paramsList = paramsRepository.findByProviderAndGrupo(
                Constantes.PROVIDER_ALFIN,
                Constantes.GRUPO_CONSULTAS
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAlterno);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Usuario", usuario);

        headers.set("token", sessionToken);

        paramsList.stream()
                .filter(param -> !"alfinhash".equalsIgnoreCase(param.getCodigo()))
                .forEach(param -> headers.set(param.getCodigo(), param.getValor()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ClienteBaaS", usuario);
        payload.put("CuentaBaaS", numeroCuenta);
        payload.put("FechaDesde", fechaInicio);
        payload.put("FechaHasta", fechaFin);
        payload.put("CantidadMovimientos", 100);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AlfinException("No se pudo serializar el payload para firmar alfinhash");
        }

        String alfinHashSecret = paramsList.stream()
                .filter(param -> "alfinhash".equalsIgnoreCase(param.getCodigo()))
                .map(Params::getValor)
                .findFirst()
                .orElseThrow(() -> new AlfinException("Clave 'alfinhash' no configurada en parámetros ALFIN"));

        String alfinHash = hmacSha256Hex(payloadJson, alfinHashSecret);
        headers.set("alfinhash", alfinHash);

        String url = String.format("%s/api/ABServices/v1/ObtenerMovimientosFechaCuentaBaaS", alfinBaseUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<MovimientosResponse> response =
                    restTemplate.postForEntity(url, request, MovimientosResponse.class);

            MovimientosResponse body = response.getBody();
            if (body == null || body.getMovimientos() == null || body.getMovimientos().isEmpty()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> movimientos = new ArrayList<>();
            for (Movimiento movimiento : body.getMovimientos()) {
                movimientos.add(mapearMovimiento(movimiento));
            }

            log.debug("Movimientos ALFIN obtenidos: {}", movimientos);
            return movimientos;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al invocar servicio protegido ALFIN {}: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AlfinException("Error al invocar servicio protegido ALFIN: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Error inesperado al invocar servicio protegido ALFIN: {}", e.getMessage());
            throw new AlfinException("Error inesperado al invocar servicio protegido ALFIN: " + e.getMessage());
        }
    }

    private Map<String, Object> mapearMovimiento(Movimiento movimiento) {
        Map<String, Object> map = new HashMap<>();

        String fechaFormateada = formatearFecha(movimiento.getFecha());
        map.put(Constantes.KEY_FECHA, fechaFormateada);
        map.put(Constantes.KEY_FECHA_VALOR, fechaFormateada);

        map.put(Constantes.KEY_OPERACION,
                Optional.ofNullable(movimiento.getMovimientoUId()).orElse(""));
        map.put(Constantes.KEY_DESCRIPCION,
                Optional.ofNullable(movimiento.getConcepto()).orElse("").replaceAll("\\s+", " ").trim());

        double monto = Optional.ofNullable(movimiento.getImporte()).orElse(0.0);
        if ("D".equalsIgnoreCase(movimiento.getDebitoCredito())) {
            monto = -Math.abs(monto);
            map.put("tipo", "DEBITO");
        } else {
            monto = Math.abs(monto);
            map.put("tipo", "CREDITO");
        }
        map.put("monto", monto);

        map.put(Constantes.KEY_REFERENCIA,
                Optional.ofNullable(movimiento.getReferencia()).orElse(""));
        map.put("saldo", "0.00");

        String moneda = normalizarMoneda(movimiento.getMoneda());
        map.put(Constantes.KEY_MONEDA, moneda);

        return map;
    }

    private String normalizarMoneda(String moneda) {
        if (!StringUtils.hasText(moneda)) {
            return null;
        }
        String valor = moneda.trim().toUpperCase(Locale.ROOT);

        if (valor.contains("S/")) {
            return "SOLES";
        }
        if (valor.contains("$") || valor.contains("USD")) {
            return "DOLARES";
        }
        return valor;
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("No fue posible calcular el alfinhash", e);
        }
    }

    private record TokenResult(boolean success,
                               String token,
                               String userMessage,
                               String errorCode,
                               String errorMessage) {

        static TokenResult success(String token) {
            return new TokenResult(true, token, null, null, null);
        }

        static TokenResult failure(String errorCode, String userMessage, String errorMessage) {
            return new TokenResult(false, null, userMessage, errorCode, errorMessage);
        }
    }

    private String formatearFecha(String fechaIso) {
        if (!StringUtils.hasText(fechaIso)) {
            return "";
        }
        try {
            LocalDate fecha = LocalDate.parse(fechaIso, FORMATO_ENTRADA);
            return fecha.format(FORMATO_SALIDA);
        } catch (DateTimeParseException e) {
            log.warn("No se pudo formatear la fecha '{}': {}", fechaIso, e.getMessage());
            return fechaIso; // fallback al valor original si hay error
        }
    }
}
