package br.com.vistoriapredial.vistoria.application.command;

import br.com.vistoriapredial.vistoria.domain.ItemRoteiroVistoria;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;

public record AmbienteRoteiroCommand(
        Long id,
        TipoAmbiente tipo,
        String nome
) {
    public ItemRoteiroVistoria toDomain() {
        return new ItemRoteiroVistoria(id, tipo, nome);
    }
}
