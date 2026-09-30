package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultadoAnaliseCalculatorTest {

    private final ResultadoAnaliseCalculator calculator = new ResultadoAnaliseCalculator();

    @Test
    void retornaInconclusivoQuandoNaoHaEvidencias() {
        ResultadoAnaliseCalculator.ResultadoCalculado resultado = calculator.calcular(List.of());

        assertThat(resultado.resultadoGeral()).isEqualTo(AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO);
        assertThat(resultado.ambientes()).isEmpty();
    }

    @Test
    void retornaInconclusivoQuandoAmbienteNaoTemEvidenciaSuficiente() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO);
    }

    @Test
    void aprovaAmbienteComEvidenciaSuficienteSemAchados() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO);
    }

    @Test
    void aprovaComRessalvasQuandoMaiorGravidadeEhBaixa() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                AnaliseVistoria.GravidadeAchado.BAIXA)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO_COM_RESSALVAS);
    }

    @Test
    void aprovaComRessalvasQuandoMaiorGravidadeEhMedia() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                AnaliseVistoria.GravidadeAchado.BAIXA,
                AnaliseVistoria.GravidadeAchado.MEDIA)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO_COM_RESSALVAS);
    }

    @Test
    void naoAprovaQuandoExisteGravidadeAlta() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                AnaliseVistoria.GravidadeAchado.ALTA)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.NAO_APROVADO);
    }

    @Test
    void naoAprovaQuandoExisteGravidadeCritica() {
        var resultado = calculator.calcular(List.of(imagem(1L,
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                AnaliseVistoria.GravidadeAchado.CRITICA)));

        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.NAO_APROVADO);
    }

    @Test
    void usaEvidenciaSuficienteAlternativaNoMesmoAmbiente() {
        var resultado = calculator.calcular(List.of(
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE,
                        AnaliseVistoria.GravidadeAchado.CRITICA),
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE)));

        assertThat(resultado.ambientes()).hasSize(1);
        assertThat(resultado.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO);
    }

    @Test
    void resultadoGeralPriorizaNaoAprovado() {
        var resultado = calculator.calcular(List.of(
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE),
                imagem(2L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                        AnaliseVistoria.GravidadeAchado.ALTA)));

        assertThat(resultado.resultadoGeral()).isEqualTo(AnaliseVistoria.ResultadoAnalise.NAO_APROVADO);
    }

    @Test
    void resultadoGeralPriorizaInconclusivoSobreRessalvas() {
        var resultado = calculator.calcular(List.of(
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE),
                imagem(2L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                        AnaliseVistoria.GravidadeAchado.MEDIA)));

        assertThat(resultado.resultadoGeral()).isEqualTo(AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO);
    }

    @Test
    void resultadoGeralPriorizaRessalvasSobreAprovado() {
        var resultado = calculator.calcular(List.of(
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE),
                imagem(2L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                        AnaliseVistoria.GravidadeAchado.BAIXA)));

        assertThat(resultado.resultadoGeral())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO_COM_RESSALVAS);
    }

    @Test
    void resultadoGeralEhAprovadoQuandoTodosAmbientesSaoAprovados() {
        var resultado = calculator.calcular(List.of(
                imagem(1L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE),
                imagem(2L, AnaliseVistoria.QualidadeEvidencia.SUFICIENTE)));

        assertThat(resultado.resultadoGeral()).isEqualTo(AnaliseVistoria.ResultadoAnalise.APROVADO);
        assertThat(resultado.motivoResultadoGeral()).isNotBlank();
    }

    private AnaliseVistoria.ImagemAnalise imagem(
            long ambienteId,
            AnaliseVistoria.QualidadeEvidencia qualidade,
            AnaliseVistoria.GravidadeAchado... gravidades) {
        List<AnaliseVistoria.AchadoIa> achados = java.util.Arrays.stream(gravidades)
                .map(gravidade -> achado(gravidade, achadosIndice(gravidade)))
                .toList();
        return new AnaliseVistoria.ImagemAnalise(
                ambienteId * 10,
                "vistorias/7/ambiente-" + ambienteId + ".jpg",
                "ana-" + ambienteId,
                new AnaliseVistoria.AmbienteImagem(
                        ambienteId, "Ambiente " + ambienteId, CategoriaEvidencia.VISAO_GERAL),
                "Resumo",
                List.of(),
                qualidade == AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE
                        ? "Faça uma nova captura." : null,
                new AnaliseVistoria.QualidadeImagem(qualidade, List.of()),
                achados);
    }

    private int achadosIndice(AnaliseVistoria.GravidadeAchado gravidade) {
        return gravidade.ordinal();
    }

    private AnaliseVistoria.AchadoIa achado(AnaliseVistoria.GravidadeAchado gravidade, int indice) {
        return new AnaliseVistoria.AchadoIa(
                indice, "Critério", "Parede", "INDICIO", "Descrição", "Evidência",
                "Impacto", gravidade, AnaliseVistoria.ConfiancaAchado.ALTA,
                "Recomendação", "Parede ao fundo");
    }
}
