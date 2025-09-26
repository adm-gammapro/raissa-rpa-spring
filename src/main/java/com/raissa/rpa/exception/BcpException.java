package com.raissa.rpa.exception;

import lombok.Getter;

@Getter
public class BcpException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;

    public BcpException(String message) {
        super(message);
        this.errorCode = "BCP_GENERIC_ERROR";
        this.userMessage = "Error en el proceso del BCP";
    }

    public BcpException(String message, String errorCode, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public BcpException(String message, Throwable cause, String errorCode, String userMessage) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    // Métodos de fábrica para errores específicos
    public static BcpException elementNotFound(String elementName, String selector) {
        String message = String.format("No se pudo encontrar el elemento '%s' con selector: %s",
                elementName, selector);
        return new BcpException(message, "BCP_ELEMENT_NOT_FOUND",
                "La página del banco ha cambiado. Por favor, contacte al administrador.");
    }

    public static BcpException pageNotLoaded(String url) {
        String message = String.format("No se pudo cargar la página del BCP: %s", url);
        return new BcpException(message, "BCP_PAGE_NOT_LOADED",
                "No se pudo acceder al portal del banco. Intente nuevamente.");
    }

    public static BcpException invalidCredentials() {
        return new BcpException("Credenciales inválidas en el portal BCP",
                "BCP_INVALID_CREDENTIALS",
                "Las credenciales del banco son incorrectas. Verifique e intente nuevamente.");
    }

    public static BcpException pageUpdated() {
        return new BcpException("La página del BCP ha sido actualizada",
                "BCP_PAGE_UPDATED",
                "El portal del banco ha cambiado. Se requiere actualización del sistema.");
    }

    @Override
    public String toString() {
        return String.format("BcpException{errorCode='%s', userMessage='%s', message='%s'}",
                errorCode, userMessage, getMessage());
    }
}