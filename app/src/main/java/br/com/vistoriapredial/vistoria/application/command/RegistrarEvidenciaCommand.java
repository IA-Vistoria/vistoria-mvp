package br.com.vistoriapredial.vistoria.application.command;

public record RegistrarEvidenciaCommand(
        Long ambienteId,
        String categoria
) {
}
