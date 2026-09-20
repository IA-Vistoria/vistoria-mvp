package br.com.vistoriapredial.usuario.application.exception;

public class EngineerRegistrationDeniedException extends RuntimeException {

    public EngineerRegistrationDeniedException() {
        super("Cadastro profissional não autorizado");
    }
}
