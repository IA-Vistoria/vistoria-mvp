package br.com.vistoriapredial.vistoria.application.exception;

public class VistoriaAccessDeniedException extends RuntimeException {

    public VistoriaAccessDeniedException() {
        super("Você não tem permissão para acessar esta vistoria.");
    }
}
