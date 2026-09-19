package br.com.vistoriapredial.vistoria.application.exception;

public class EvidenceNotFoundException extends RuntimeException {

    public EvidenceNotFoundException() {
        super("Evidência não encontrada.");
    }
}
