package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;

import java.time.LocalDateTime;

public record ImagemVistoriaResponseDto(
        Long id,
        String protocoloItem,
        LocalDateTime dataUpload,
        String conteudoUrl
) {
    public static ImagemVistoriaResponseDto from(Long vistoriaId, ImagemVistoria imagem) {
        return new ImagemVistoriaResponseDto(
                imagem.getId(),
                imagem.getProtocoloItem(),
                imagem.getDataUpload(),
                "/api/vistorias/" + vistoriaId + "/imagens/" + imagem.getId() + "/conteudo"
        );
    }
}
