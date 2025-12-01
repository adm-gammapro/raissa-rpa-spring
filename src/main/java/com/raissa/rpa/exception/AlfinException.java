package com.raissa.rpa.exception;

import lombok.Getter;

@Getter
public class AlfinException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;

    public AlfinException(String message) {
        super(message);
        this.errorCode = "ALFIN_GENERIC_ERROR";
        this.userMessage = "Error en el proceso del ALFIN";
    }

    public AlfinException(String message, String errorCode, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public AlfinException(String message, Throwable cause, String errorCode, String userMessage) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    // Métodos de fábrica para errores específicos
    public static AlfinException elementNotFound(String elementName, String selector) {
        String message = String.format("No se pudo encontrar el elemento '%s' con selector: %s",
                elementName, selector);
        return new AlfinException(message, "ALFIN_ELEMENT_NOT_FOUND",
                "La página del banco ha cambiado. Por favor, contacte al administrador.");
    }

    public static AlfinException pageNotLoaded(String url) {
        String message = String.format("No se pudo cargar la página del ALFIN: %s", url);
        return new AlfinException(message, "ALFIN_PAGE_NOT_LOADED",
                "No se pudo acceder al portal del banco. Intente nuevamente.");
    }

    public static AlfinException invalidCredentials() {
        return new AlfinException("Credenciales inválidas en el portal ALFIN",
                "ALFIN_INVALID_CREDENTIALS",
                "Las credenciales del banco son incorrectas. Verifique e intente nuevamente.");
    }

    public static AlfinException pageUpdated() {
        return new AlfinException("La página del ALFIN ha sido actualizada",
                "ALFIN_PAGE_UPDATED",
                "El portal del banco ha cambiado. Se requiere actualización del sistema.");
    }

    @Override
    public String toString() {
        return String.format("AlfinException{errorCode='%s', userMessage='%s', message='%s'}",
                errorCode, userMessage, getMessage());
    }
}
