package br.com.vistoriapredial.vistoria.application.analysis;

import java.util.List;

public record AnaliseVistoria(
        int version,
        List<ImagemAnalise> imagens
) {
    public record ImagemAnalise(
            Long imagemId,
            String identificadorAnalise,
            String resumoGeral,
            List<String> limitacoes,
            QualidadeImagem qualidade,
            List<AchadoIa> achados
    ) {
    }

    public record QualidadeImagem(
            boolean utilizavel,
            List<String> problemas
    ) {
    }

    public record AchadoIa(
            int indice,
            String area,
            String tipo,
            String descricao,
            String evidencia,
            String gravidade,
            String confianca,
            String recomendacao,
            String localizacao
    ) {
    }
}
