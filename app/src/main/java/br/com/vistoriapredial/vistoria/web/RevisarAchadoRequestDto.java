package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.review.DecisaoRevisao;
import br.com.vistoriapredial.vistoria.application.review.RevisarAchadoCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RevisarAchadoRequestDto(
        @NotNull(message = "A evidência é obrigatória")
        @Positive(message = "A evidência deve ser válida")
        Long imagemId,
        @NotNull(message = "O índice do achado é obrigatório")
        @PositiveOrZero(message = "O índice do achado deve ser válido")
        Integer indiceAchado,
        @NotNull(message = "A decisão é obrigatória")
        DecisaoRevisao decisao,
        @Size(max = 1000, message = "O contexto deve ter no máximo 1000 caracteres")
        String contexto
) {
    public RevisarAchadoCommand toCommand() {
        return new RevisarAchadoCommand(
                imagemId, indiceAchado, decisao, contexto, null);
    }
}
