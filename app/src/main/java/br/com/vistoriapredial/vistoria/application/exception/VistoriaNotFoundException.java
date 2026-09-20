package br.com.vistoriapredial.vistoria.application.exception;

public class VistoriaNotFoundException extends RuntimeException {

    public VistoriaNotFoundException() {
        super("Vistoria não encontrada.");
    }
}
