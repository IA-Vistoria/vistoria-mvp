package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.command.AmbienteRoteiroCommand;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AmbienteRoteiroRequestDto(
        @Positive(message = "O identificador do ambiente deve ser positivo")
        Long id,

        @NotNull(message = "O tipo do ambiente é obrigatório")
        TipoAmbiente tipo,

        @NotBlank(message = "O nome do ambiente é obrigatório")
        @Size(min = 2, max = 60, message = "O nome do ambiente deve ter entre 2 e 60 caracteres")
        String nome
) {
    public AmbienteRoteiroCommand toCommand() {
        return new AmbienteRoteiroCommand(id, tipo, nome);
    }
}
