package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;

import java.time.Instant;
import java.util.List;

public record AnaliseVistoria(
        int version,
        ExecucaoAnalise execucao,
        List<ImagemAnalise> imagens,
        List<AmbienteResultado> ambientes,
        ResultadoAnalise resultadoGeral,
        String motivoResultadoGeral) {

    public AnaliseVistoria {
        imagens = copia(imagens);
        ambientes = copia(ambientes);
    }

    public AnaliseVistoria(int version, List<ImagemAnalise> imagens) {
        this(version, null, imagens, List.of(), null, null);
    }

    public record ExecucaoAnalise(
            String provider,
            String modelo,
            String versaoPrompt,
            String identificadorAnalise,
            Instant concluidaEm) {
    }

    public record ImagemAnalise(
            Long imagemId,
            String storagePath,
            String identificadorAnalise,
            AmbienteImagem ambiente,
            String resumoGeral,
            List<String> limitacoes,
            String orientacaoNovaCaptura,
            QualidadeImagem qualidade,
            List<AchadoIa> achados) {

        public ImagemAnalise {
            limitacoes = copia(limitacoes);
            achados = copia(achados);
        }

        public ImagemAnalise(
                Long imagemId,
                String identificadorAnalise,
                String resumoGeral,
                List<String> limitacoes,
                QualidadeImagem qualidade,
                List<AchadoIa> achados) {
            this(imagemId, null, identificadorAnalise, null, resumoGeral,
                    limitacoes, null, qualidade, achados);
        }
    }

    public record AmbienteImagem(
            Long id,
            String nome,
            CategoriaEvidencia categoria) {
    }

    public record AmbienteResultado(
            Long id,
            String nome,
            ResultadoAnalise resultado,
            String motivoResultado) {
    }

    public record QualidadeImagem(
            Boolean utilizavel,
            QualidadeEvidencia nivel,
            List<String> problemas) {

        public QualidadeImagem {
            problemas = copia(problemas);
        }

        public QualidadeImagem(boolean utilizavel, List<String> problemas) {
            this(utilizavel, null, problemas);
        }

        public QualidadeImagem(QualidadeEvidencia nivel, List<String> problemas) {
            this(nivel == QualidadeEvidencia.SUFICIENTE, nivel, problemas);
        }
    }

    public record AchadoIa(
            int indice,
            String criterio,
            String area,
            String tipo,
            String descricao,
            String evidencia,
            String impacto,
            String gravidade,
            GravidadeAchado gravidadeNormalizada,
            String confianca,
            ConfiancaAchado confiancaNormalizada,
            String recomendacao,
            String localizacao) {

        public AchadoIa(
                int indice,
                String area,
                String tipo,
                String descricao,
                String evidencia,
                String gravidade,
                String confianca,
                String recomendacao,
                String localizacao) {
            this(indice, null, area, tipo, descricao, evidencia, null,
                    gravidade, enumOpcional(GravidadeAchado.class, gravidade),
                    confianca, enumOpcional(ConfiancaAchado.class, confianca),
                    recomendacao, localizacao);
        }

        public AchadoIa(
                int indice,
                String criterio,
                String area,
                String tipo,
                String descricao,
                String evidencia,
                String impacto,
                GravidadeAchado gravidade,
                ConfiancaAchado confianca,
                String recomendacao,
                String localizacao) {
            this(indice, criterio, area, tipo, descricao, evidencia, impacto,
                    gravidade == null ? null : gravidade.name(), gravidade,
                    confianca == null ? null : confianca.name(), confianca,
                    recomendacao, localizacao);
        }
    }

    public enum ResultadoAnalise {
        APROVADO,
        APROVADO_COM_RESSALVAS,
        NAO_APROVADO,
        INCONCLUSIVO
    }

    public enum QualidadeEvidencia {
        SUFICIENTE,
        INSUFICIENTE
    }

    public enum GravidadeAchado {
        BAIXA,
        MEDIA,
        ALTA,
        CRITICA
    }

    public enum ConfiancaAchado {
        BAIXA,
        MEDIA,
        ALTA
    }

    private static <T> List<T> copia(List<T> valores) {
        return valores == null ? List.of() : List.copyOf(valores);
    }

    private static <T extends Enum<T>> T enumOpcional(Class<T> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
