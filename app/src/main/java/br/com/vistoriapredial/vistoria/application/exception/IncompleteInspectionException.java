package br.com.vistoriapredial.vistoria.application.exception;

import java.util.List;

public class IncompleteInspectionException extends RuntimeException {

    private final List<String> ambientesAusentes;

    public IncompleteInspectionException(List<String> ambientesAusentes) {
        super("Adicione uma visão geral para cada ambiente antes de enviar a vistoria.");
        this.ambientesAusentes = List.copyOf(ambientesAusentes);
    }

    public List<String> getAmbientesAusentes() {
        return ambientesAusentes;
    }
}
