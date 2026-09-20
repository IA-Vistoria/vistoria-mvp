package br.com.vistoriapredial.vistoria.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarVistoriaRequestDto(
        @NotBlank(message = "O endereço não pode estar vazio")
        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres")
        String endereco
) {}
