package br.com.vistoriapredial.vistoria.web;

import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;

import java.time.LocalDateTime;

public record ImagemVistoriaResponseDto(
        Long id,
        Long ambienteId,
        String ambienteNome,
        CategoriaEvidencia categoria,
        String protocoloItem,
        LocalDateTime dataUpload,
        String conteudoUrl
) {
    public static ImagemVistoriaResponseDto from(Long vistoriaId, ImagemVistoria imagem) {
        return new ImagemVistoriaResponseDto(
                imagem.getId(),
                imagem.getAmbiente() == null ? null : imagem.getAmbiente().getId(),
                imagem.getAmbiente() == null ? null : imagem.getAmbiente().getNome(),
                imagem.getCategoria(),
                imagem.getProtocoloItem(),
                imagem.getDataUpload(),
                "/api/vistorias/" + vistoriaId + "/imagens/" + imagem.getId() + "/conteudo"
        );
    }
}
