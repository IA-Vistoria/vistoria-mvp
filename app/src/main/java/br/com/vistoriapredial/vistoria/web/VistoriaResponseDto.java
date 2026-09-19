package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;

import java.time.LocalDateTime;

public record VistoriaResponseDto(
        Long id,
        Long clienteId,
        Long engenheiroId,
        VistoriaStatus status,
        String preLaudoIa,
        String parecerEngenheiro,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao
) {
    public static VistoriaResponseDto from(Vistoria v) {
        return new VistoriaResponseDto(
                v.getId(),
                v.getCliente().getId(),
                v.getEngenheiro() != null ? v.getEngenheiro().getId() : null,
                v.getStatus(),
                v.getPreLaudoIa(),
                v.getParecerEngenheiro(),
                v.getDataCriacao(),
                v.getDataConclusao()
        );
    }
}
