package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import br.com.vistoriapredial.vistoria.application.review.RevisaoAchado;

import java.time.LocalDateTime;
import java.util.List;

public record VistoriaResponseDto(
        Long id,
        Long version,
        Long clienteId,
        VistoriaStatus status,
        String endereco,
        TipoImovel tipoImovel,
        List<AmbienteVistoriaResponseDto> ambientes,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao,
        List<ImagemVistoriaResponseDto> imagens,
        AnaliseVistoria analiseIa,
        List<RevisaoAchado> manifestacoes
) {
}
