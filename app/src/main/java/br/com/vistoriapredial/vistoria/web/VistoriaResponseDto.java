package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.domain.Vistoria;
import br.com.vistoriapredial.vistoria.domain.VistoriaStatus;

import java.time.LocalDateTime;
import java.util.List;

public record VistoriaResponseDto(
        Long id,
        Long clienteId,
        Long engenheiroId,
        VistoriaStatus status,
        String preLaudoIa,
        String parecerEngenheiro,
        String endereco,
        LocalDateTime dataCriacao,
        LocalDateTime dataConclusao,
        List<ImagemVistoriaResponseDto> imagens
) {
    public static VistoriaResponseDto from(Vistoria v) {
        return new VistoriaResponseDto(
                v.getId(),
                v.getCliente().getId(),
                v.getEngenheiro() != null ? v.getEngenheiro().getId() : null,
                v.getStatus(),
                v.getPreLaudoIa(),
                v.getParecerEngenheiro(),
                v.getEndereco(),
                v.getDataCriacao(),
                v.getDataConclusao(),
                v.getImagens().stream()
                        .map(ImagemVistoriaResponseDto::from)
                        .toList()
        );
    }
}
