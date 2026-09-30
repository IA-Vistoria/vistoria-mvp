package br.com.vistoriapredial.vistoria.application.exception;

public class InvalidAiAnalysisException extends RuntimeException {

    public InvalidAiAnalysisException(String message) {
        super(message);
    }

    public InvalidAiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
