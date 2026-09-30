package br.com.vistoriapredial.vistoria.application.command;

import br.com.vistoriapredial.vistoria.domain.TipoImovel;

import java.util.List;

public record CriarVistoriaCommand(
        String endereco,
        TipoImovel tipoImovel,
        List<AmbienteRoteiroCommand> ambientes
) {
}
