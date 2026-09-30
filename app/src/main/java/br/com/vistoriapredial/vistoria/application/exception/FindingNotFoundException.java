package br.com.vistoriapredial.vistoria.application.exception;

public class FindingNotFoundException extends RuntimeException {

    public FindingNotFoundException() {
        super("O achado informado não pertence à análise desta vistoria.");
    }
}
