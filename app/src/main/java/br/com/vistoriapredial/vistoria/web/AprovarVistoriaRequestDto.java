package br.com.vistoriapredial.vistoria.web;

import jakarta.validation.constraints.NotBlank;

public record AprovarVistoriaRequestDto(
        @NotBlank(message = "Parecer é obrigatório")
        String parecer,
        boolean aprovado
) {}
