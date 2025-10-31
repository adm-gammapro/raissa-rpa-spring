package com.raissa.rpa.exception;

public class BbvaException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;

    public BbvaException(String message) {
        super(message);
        this.errorCode = "BBVA_GENERIC_ERROR";
        this.userMessage = "Error en el proceso del BBVA";
    }

    public BbvaException(String message, String errorCode, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public BbvaException(String message, Throwable cause, String errorCode, String userMessage) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    // Métodos de fábrica para errores específicos
    public static BbvaException elementNotFound(String elementName, String selector) {
        String message = String.format("No se pudo encontrar el elemento '%s' con selector: %s",
                elementName, selector);
        return new BbvaException(message, "BBVA_ELEMENT_NOT_FOUND",
                "La página del banco ha cambiado. Por favor, contacte al administrador.");
    }

    public static BbvaException pageNotLoaded(String url) {
        String message = String.format("No se pudo cargar la página del BBVA: %s", url);
        return new BbvaException(message, "BBVA_PAGE_NOT_LOADED",
                "No se pudo acceder al portal del banco. Intente nuevamente.");
    }

    public static BbvaException invalidCredentials() {
        return new BbvaException("Credenciales inválidas en el portal BBVA",
                "BBVA_INVALID_CREDENTIALS",
                "Las credenciales del banco son incorrectas. Verifique e intente nuevamente.");
    }

    public static BbvaException pageUpdated() {
        return new BbvaException("La página del BBVA ha sido actualizada",
                "BBVA_PAGE_UPDATED",
                "El portal del banco ha cambiado. Se requiere actualización del sistema.");
    }

    @Override
    public String toString() {
        return String.format("BbvaException{errorCode='%s', userMessage='%s', message='%s'}",
                errorCode, userMessage, getMessage());
    }
}
