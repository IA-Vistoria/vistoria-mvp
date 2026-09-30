package br.com.vistoriapredial.vistoria.application.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResultadoAnaliseCalculator {

    public ResultadoCalculado calcular(List<AnaliseVistoria.ImagemAnalise> imagens) {
        if (imagens == null) {
            throw new IllegalArgumentException("As imagens da análise são obrigatórias.");
        }

        Map<Long, GrupoAmbiente> grupos = new LinkedHashMap<>();
        for (AnaliseVistoria.ImagemAnalise imagem : imagens) {
            if (imagem.ambiente() == null || imagem.ambiente().id() == null) {
                throw new IllegalArgumentException("A análise v2 exige o ambiente de cada imagem.");
            }
            grupos.computeIfAbsent(imagem.ambiente().id(), ignored -> new GrupoAmbiente(
                    imagem.ambiente().id(), imagem.ambiente().nome(), new ArrayList<>()))
                    .imagens().add(imagem);
        }

        List<AnaliseVistoria.AmbienteResultado> ambientes = grupos.values().stream()
                .map(this::calcularAmbiente)
                .toList();
        AnaliseVistoria.ResultadoAnalise resultadoGeral = ambientes.stream()
                .map(AnaliseVistoria.AmbienteResultado::resultado)
                .min(java.util.Comparator.comparingInt(this::prioridade))
                .orElse(AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO);

        return new ResultadoCalculado(
                ambientes,
                resultadoGeral,
                motivoGeral(resultadoGeral));
    }

    private AnaliseVistoria.AmbienteResultado calcularAmbiente(GrupoAmbiente grupo) {
        List<AnaliseVistoria.ImagemAnalise> suficientes = grupo.imagens().stream()
                .filter(imagem -> imagem.qualidade() != null)
                .filter(imagem -> imagem.qualidade().nivel()
                        == AnaliseVistoria.QualidadeEvidencia.SUFICIENTE)
                .toList();

        AnaliseVistoria.ResultadoAnalise resultado;
        String motivo;
        if (suficientes.isEmpty()) {
            resultado = AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO;
            motivo = "Nenhuma evidência suficiente permite concluir a análise do ambiente.";
        } else {
            List<AnaliseVistoria.AchadoIa> achados = suficientes.stream()
                    .flatMap(imagem -> imagem.achados().stream())
                    .toList();
            boolean temAltaOuCritica = achados.stream()
                    .map(AnaliseVistoria.AchadoIa::gravidadeNormalizada)
                    .anyMatch(gravidade -> gravidade == AnaliseVistoria.GravidadeAchado.ALTA
                            || gravidade == AnaliseVistoria.GravidadeAchado.CRITICA);
            if (temAltaOuCritica) {
                resultado = AnaliseVistoria.ResultadoAnalise.NAO_APROVADO;
                motivo = "Foi identificado ao menos um achado de alta ou crítica gravidade.";
            } else if (!achados.isEmpty()) {
                resultado = AnaliseVistoria.ResultadoAnalise.APROVADO_COM_RESSALVAS;
                motivo = "Foram identificados achados de baixa ou média gravidade.";
            } else {
                resultado = AnaliseVistoria.ResultadoAnalise.APROVADO;
                motivo = "As evidências suficientes não apresentaram achados visuais.";
            }
        }

        return new AnaliseVistoria.AmbienteResultado(
                grupo.id(), grupo.nome(), resultado, motivo);
    }

    private int prioridade(AnaliseVistoria.ResultadoAnalise resultado) {
        return switch (resultado) {
            case NAO_APROVADO -> 0;
            case INCONCLUSIVO -> 1;
            case APROVADO_COM_RESSALVAS -> 2;
            case APROVADO -> 3;
        };
    }

    private String motivoGeral(AnaliseVistoria.ResultadoAnalise resultado) {
        return switch (resultado) {
            case NAO_APROVADO -> "Ao menos um ambiente possui achado de alta ou crítica gravidade.";
            case INCONCLUSIVO -> "Ao menos um ambiente não possui evidência suficiente para conclusão.";
            case APROVADO_COM_RESSALVAS -> "Ao menos um ambiente possui achados de baixa ou média gravidade.";
            case APROVADO -> "Todos os ambientes analisados possuem evidências suficientes sem achados visuais.";
        };
    }

    public record ResultadoCalculado(
            List<AnaliseVistoria.AmbienteResultado> ambientes,
            AnaliseVistoria.ResultadoAnalise resultadoGeral,
            String motivoResultadoGeral) {

        public ResultadoCalculado {
            ambientes = List.copyOf(ambientes);
        }
    }

    private record GrupoAmbiente(
            Long id,
            String nome,
            List<AnaliseVistoria.ImagemAnalise> imagens) {
    }
}
