package br.com.vistoriapredial.vistoria.application.exception;

public class IncompleteReviewException extends RuntimeException {

    public IncompleteReviewException() {
        super("Revise todos os achados antes de gerar o relatório.");
    }
}
