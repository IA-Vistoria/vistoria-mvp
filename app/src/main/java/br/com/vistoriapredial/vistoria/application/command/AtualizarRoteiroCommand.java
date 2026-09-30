package br.com.vistoriapredial.vistoria.application.command;

import br.com.vistoriapredial.vistoria.domain.TipoImovel;

import java.util.List;

public record AtualizarRoteiroCommand(
        Long version,
        TipoImovel tipoImovel,
        List<AmbienteRoteiroCommand> ambientes
) {
}
