package br.com.vistoriapredial.vistoria.web;

import jakarta.validation.constraints.NotBlank;

public record CriarVistoriaRequestDto(
        @NotBlank(message = "O endereço não pode estar vazio")
        String endereco
) {}
