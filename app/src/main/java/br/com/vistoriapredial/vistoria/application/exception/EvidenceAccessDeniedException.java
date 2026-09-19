package br.com.vistoriapredial.vistoria.application.exception;

public class EvidenceAccessDeniedException extends RuntimeException {

    public EvidenceAccessDeniedException() {
        super("Você não tem permissão para acessar esta evidência.");
    }
}
