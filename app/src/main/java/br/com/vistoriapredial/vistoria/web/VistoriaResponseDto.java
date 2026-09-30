package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;

import java.time.LocalDateTime;
import java.util.List;

public record VistoriaResponseDto(
        Long id,
        Long clienteId,
        VistoriaStatus status,
        String endereco,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao,
        List<ImagemVistoriaResponseDto> imagens,
        AnaliseVistoria analiseIa
) {
}
