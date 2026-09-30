package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;

public record AmbienteVistoriaResponseDto(
        Long id,
        TipoAmbiente tipo,
        String nome,
        int ordem
) {
    public static AmbienteVistoriaResponseDto from(AmbienteVistoria ambiente) {
        return new AmbienteVistoriaResponseDto(
                ambiente.getId(),
                ambiente.getTipo(),
                ambiente.getNome(),
                ambiente.getOrdem());
    }
}
