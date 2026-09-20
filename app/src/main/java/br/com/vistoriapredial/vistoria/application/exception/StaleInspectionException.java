package br.com.vistoriapredial.vistoria.application.exception;

public class StaleInspectionException extends RuntimeException {

    public StaleInspectionException() {
        super("A vistoria não está mais disponível para esta operação.");
    }
}
