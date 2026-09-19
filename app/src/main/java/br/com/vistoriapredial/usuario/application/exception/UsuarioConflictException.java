package br.com.vistoriapredial.usuario.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UsuarioConflictException extends RuntimeException {
    public UsuarioConflictException(String message) {
        super(message);
    }
}
