package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnaliseVistoriaDocumentFactory {

    private static final int VERSION = 2;
    private static final int MAX_TEXT_LENGTH = 2_000;

    private final ObjectMapper objectMapper;
    private final ResultadoAnaliseCalculator resultadoCalculator;

    public AnaliseVistoriaDocumentFactory(
            ObjectMapper objectMapper,
            ResultadoAnaliseCalculator resultadoCalculator) {
        this.objectMapper = objectMapper;
        this.resultadoCalculator = resultadoCalculator;
    }

    public String criar(
            SolicitacaoAnaliseIa solicitacao,
            MetadadosExecucao execucao,
            List<ObservacaoImagem> observacoes) {
        if (solicitacao == null || execucao == null || observacoes == null) {
            throw new IllegalArgumentException("Solicitação, execução e observações são obrigatórias.");
        }

        Map<String, EvidenciaAnaliseIa> evidenciasPorCaminho = new LinkedHashMap<>();
        for (EvidenciaAnaliseIa evidencia : solicitacao.evidencias()) {
            if (evidenciasPorCaminho.put(evidencia.storagePath(), evidencia) != null) {
                throw new IllegalArgumentException(
                        "Cada evidência deve possuir exatamente uma observação.");
            }
        }

        Map<String, ObservacaoImagem> observacoesPorCaminho = new LinkedHashMap<>();
        for (ObservacaoImagem observacao : observacoes) {
            if (observacao == null) {
                throw new IllegalArgumentException("As observações não aceitam itens nulos.");
            }
            if (!evidenciasPorCaminho.containsKey(observacao.storagePath())) {
                throw new IllegalArgumentException(
                        "A observação referencia uma evidência desconhecida.");
            }
            if (observacoesPorCaminho.put(observacao.storagePath(), observacao) != null) {
                throw new IllegalArgumentException(
                        "Cada evidência deve possuir exatamente uma observação.");
            }
        }
        if (observacoesPorCaminho.size() != evidenciasPorCaminho.size()) {
            throw new IllegalArgumentException(
                    "Cada evidência deve possuir exatamente uma observação.");
        }

        List<AnaliseVistoria.ImagemAnalise> imagens = new ArrayList<>();
        for (EvidenciaAnaliseIa evidencia : solicitacao.evidencias()) {
            ObservacaoImagem observacao = observacoesPorCaminho.get(evidencia.storagePath());
            List<AnaliseVistoria.AchadoIa> achados = new ArrayList<>();
            for (int index = 0; index < observacao.achados().size(); index++) {
                ObservacaoAchado achado = observacao.achados().get(index);
                achados.add(new AnaliseVistoria.AchadoIa(
                        index,
                        achado.criterio(),
                        achado.area(),
                        achado.tipo(),
                        achado.descricao(),
                        achado.evidencia(),
                        achado.impacto(),
                        achado.gravidade(),
                        achado.confianca(),
                        achado.recomendacao(),
                        achado.localizacao()));
            }
            imagens.add(new AnaliseVistoria.ImagemAnalise(
                    evidencia.imagemId(),
                    evidencia.storagePath(),
                    execucao.identificadorAnalise(),
                    new AnaliseVistoria.AmbienteImagem(
                            evidencia.ambienteId(), evidencia.ambienteNome(), evidencia.categoria()),
                    observacao.resumo(),
                    observacao.limitacoes(),
                    observacao.orientacaoNovaCaptura(),
                    new AnaliseVistoria.QualidadeImagem(observacao.qualidade(), List.of()),
                    achados));
        }

        ResultadoAnaliseCalculator.ResultadoCalculado resultado =
                resultadoCalculator.calcular(imagens);
        Map<String, Object> document = documentNode(execucao, imagens, resultado);
        try {
            return objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar a análise canônica.", exception);
        }
    }

    private Map<String, Object> documentNode(
            MetadadosExecucao execucao,
            List<AnaliseVistoria.ImagemAnalise> imagens,
            ResultadoAnaliseCalculator.ResultadoCalculado resultado) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", VERSION);
        root.put("execution", executionNode(execucao));
        root.put("images", imagens.stream().map(this::imageNode).toList());
        root.put("environments", resultado.ambientes().stream()
                .map(this::environmentResultNode)
                .toList());
        root.put("overallResult", resultado.resultadoGeral().name());
        root.put("overallReason", resultado.motivoResultadoGeral());
        return root;
    }

    private Map<String, Object> executionNode(MetadadosExecucao execucao) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("provider", execucao.provider());
        node.put("model", execucao.modelo());
        node.put("promptVersion", execucao.versaoPrompt());
        node.put("analysisId", execucao.identificadorAnalise());
        node.put("completedAt", execucao.concluidaEm().toString());
        return node;
    }

    private Map<String, Object> imageNode(AnaliseVistoria.ImagemAnalise imagem) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("imageId", imagem.imagemId());
        node.put("storagePath", imagem.storagePath());
        node.put("environment", environmentNode(imagem.ambiente()));
        node.put("imageQuality", imagem.qualidade().nivel().name());
        node.put("summary", imagem.resumoGeral());
        node.put("limitations", imagem.limitacoes());
        node.put("captureGuidance", imagem.orientacaoNovaCaptura());
        node.put("findings", imagem.achados().stream().map(this::findingNode).toList());
        return node;
    }

    private Map<String, Object> environmentNode(AnaliseVistoria.AmbienteImagem ambiente) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", ambiente.id());
        node.put("name", ambiente.nome());
        node.put("category", ambiente.categoria().name());
        return node;
    }

    private Map<String, Object> findingNode(AnaliseVistoria.AchadoIa achado) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("criterion", achado.criterio());
        node.put("area", achado.area());
        node.put("type", achado.tipo());
        node.put("description", achado.descricao());
        node.put("evidence", achado.evidencia());
        node.put("impact", achado.impacto());
        node.put("severity", achado.gravidadeNormalizada().name());
        node.put("confidence", achado.confiancaNormalizada().name());
        node.put("recommendation", achado.recomendacao());
        node.put("location", achado.localizacao());
        return node;
    }

    private Map<String, Object> environmentResultNode(
            AnaliseVistoria.AmbienteResultado ambiente) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", ambiente.id());
        node.put("name", ambiente.nome());
        node.put("result", ambiente.resultado().name());
        node.put("resultReason", ambiente.motivoResultado());
        return node;
    }

    public record MetadadosExecucao(
            String provider,
            String modelo,
            String versaoPrompt,
            String identificadorAnalise,
            Instant concluidaEm) {

        public MetadadosExecucao {
            provider = requiredText(provider, "O provedor da análise é obrigatório.");
            modelo = requiredText(modelo, "O modelo da análise é obrigatório.");
            versaoPrompt = requiredText(versaoPrompt, "A versão do prompt é obrigatória.");
            identificadorAnalise = requiredText(
                    identificadorAnalise, "O identificador da análise é obrigatório.");
            if (concluidaEm == null) {
                throw new IllegalArgumentException("O instante de conclusão é obrigatório.");
            }
        }
    }

    public record ObservacaoImagem(
            String storagePath,
            AnaliseVistoria.QualidadeEvidencia qualidade,
            String resumo,
            List<String> limitacoes,
            String orientacaoNovaCaptura,
            List<ObservacaoAchado> achados) {

        public ObservacaoImagem {
            storagePath = requiredText(storagePath, "O caminho da observação é obrigatório.");
            if (qualidade == null) {
                throw new IllegalArgumentException("A qualidade da evidência é obrigatória.");
            }
            resumo = requiredText(resumo, "O resumo da evidência é obrigatório.");
            limitacoes = copyTexts(limitacoes, "As limitações são obrigatórias.");
            orientacaoNovaCaptura = optionalText(orientacaoNovaCaptura);
            if (achados == null || achados.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("A lista de achados é obrigatória.");
            }
            achados = List.copyOf(achados);
        }
    }

    public record ObservacaoAchado(
            String criterio,
            String area,
            String tipo,
            String descricao,
            String evidencia,
            String impacto,
            AnaliseVistoria.GravidadeAchado gravidade,
            AnaliseVistoria.ConfiancaAchado confianca,
            String recomendacao,
            String localizacao) {

        public ObservacaoAchado {
            criterio = requiredText(criterio, "O critério do achado é obrigatório.");
            area = requiredText(area, "A área do achado é obrigatória.");
            tipo = requiredText(tipo, "O tipo do achado é obrigatório.");
            descricao = requiredText(descricao, "A descrição do achado é obrigatória.");
            evidencia = requiredText(evidencia, "A evidência do achado é obrigatória.");
            impacto = requiredText(impacto, "O impacto do achado é obrigatório.");
            if (gravidade == null || confianca == null) {
                throw new IllegalArgumentException("Gravidade e confiança do achado são obrigatórias.");
            }
            recomendacao = requiredText(recomendacao, "A recomendação do achado é obrigatória.");
            localizacao = requiredText(localizacao, "A localização do achado é obrigatória.");
        }
    }

    private static String requiredText(String value, String message) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("O texto da análise excede 2.000 caracteres.");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static List<String> copyTexts(List<String> values, String message) {
        if (values == null || values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(message);
        }
        return values.stream()
                .map(value -> requiredText(value, message))
                .toList();
    }
}
