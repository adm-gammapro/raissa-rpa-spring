package com.raissa.rpa.exception;

public class IbkException extends RuntimeException {
  private final String errorCode;
  private final String userMessage;

  public IbkException(String message) {
    super(message);
    this.errorCode = "IBK_GENERIC_ERROR";
    this.userMessage = "Error en el proceso del IBK";
  }

  public IbkException(String message, String errorCode, String userMessage) {
    super(message);
    this.errorCode = errorCode;
    this.userMessage = userMessage;
  }

  public IbkException(String message, Throwable cause, String errorCode, String userMessage) {
    super(message, cause);
    this.errorCode = errorCode;
    this.userMessage = userMessage;
  }

  public static IbkException elementNotFound(String elementName, String selector) {
    String message = String.format("No se pudo encontrar el elemento '%s' con selector: %s",
            elementName, selector);
    return new IbkException(message, "IBK_ELEMENT_NOT_FOUND",
            "La página del banco ha cambiado. Por favor, contacte al administrador.");
  }

  public static IbkException pageNotLoaded(String url) {
    String message = String.format("No se pudo cargar la página del IBK: %s", url);
    return new IbkException(message, "IBK_PAGE_NOT_LOADED",
            "No se pudo acceder al portal del banco. Intente nuevamente.");
  }

  public static IbkException invalidCredentials() {
    return new IbkException("Credenciales inválidas en el portal IBK",
            "IBK_INVALID_CREDENTIALS",
            "Las credenciales del banco son incorrectas. Verifique e intente nuevamente.");
  }

  public static IbkException pageUpdated() {
    return new IbkException("La página del IBK ha sido actualizada",
            "IBK_PAGE_UPDATED",
            "El portal del banco ha cambiado. Se requiere actualización del sistema.");
  }

  @Override
  public String toString() {
    return String.format("IbkException{errorCode='%s', userMessage='%s', message='%s'}",
            errorCode, userMessage, getMessage());
  }
}
