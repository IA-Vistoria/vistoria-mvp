package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.command.AtualizarRoteiroCommand;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AtualizarRoteiroRequestDto(
        @NotNull(message = "A versão da vistoria é obrigatória")
        @PositiveOrZero(message = "A versão da vistoria é inválida")
        Long version,

        @NotNull(message = "O tipo do imóvel é obrigatório")
        TipoImovel tipoImovel,

        @NotNull(message = "Os ambientes são obrigatórios")
        @Size(min = 1, max = 30, message = "O roteiro deve possuir entre 1 e 30 ambientes")
        List<@Valid AmbienteRoteiroRequestDto> ambientes
) {
    public AtualizarRoteiroCommand toCommand() {
        return new AtualizarRoteiroCommand(
                version,
                tipoImovel,
                ambientes.stream().map(AmbienteRoteiroRequestDto::toCommand).toList());
    }
}
