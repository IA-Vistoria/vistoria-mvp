package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnaliseVistoriaContractTest {

    @Test
    void fechaResultadosEClassificacoesDoContratoV2() {
        assertThat(Set.of(AnaliseVistoria.ResultadoAnalise.values())).containsExactlyInAnyOrder(
                AnaliseVistoria.ResultadoAnalise.APROVADO,
                AnaliseVistoria.ResultadoAnalise.APROVADO_COM_RESSALVAS,
                AnaliseVistoria.ResultadoAnalise.NAO_APROVADO,
                AnaliseVistoria.ResultadoAnalise.INCONCLUSIVO);
        assertThat(Set.of(AnaliseVistoria.QualidadeEvidencia.values())).containsExactlyInAnyOrder(
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE);
        assertThat(Set.of(AnaliseVistoria.GravidadeAchado.values())).containsExactlyInAnyOrder(
                AnaliseVistoria.GravidadeAchado.BAIXA,
                AnaliseVistoria.GravidadeAchado.MEDIA,
                AnaliseVistoria.GravidadeAchado.ALTA,
                AnaliseVistoria.GravidadeAchado.CRITICA);
        assertThat(Set.of(AnaliseVistoria.ConfiancaAchado.values())).containsExactlyInAnyOrder(
                AnaliseVistoria.ConfiancaAchado.BAIXA,
                AnaliseVistoria.ConfiancaAchado.MEDIA,
                AnaliseVistoria.ConfiancaAchado.ALTA);
    }

    @Test
    void representaAchadoDescritivoComClassificacoesFechadas() {
        AnaliseVistoria.AchadoIa achado = new AnaliseVistoria.AchadoIa(
                0,
                "Superfície e sinais de umidade",
                "Parede próxima ao teto",
                "UMIDADE_OU_MOFO_APARENTE",
                "Áreas escurecidas e irregulares.",
                "Manchas extensas com padrão não uniforme.",
                "Pode indicar degradação do revestimento.",
                AnaliseVistoria.GravidadeAchado.ALTA,
                AnaliseVistoria.ConfiancaAchado.ALTA,
                "Solicitar avaliação da origem da umidade.",
                "Parede ao fundo");

        assertThat(achado.criterio()).isEqualTo("Superfície e sinais de umidade");
        assertThat(achado.descricao()).isEqualTo("Áreas escurecidas e irregulares.");
        assertThat(achado.evidencia()).isEqualTo("Manchas extensas com padrão não uniforme.");
        assertThat(achado.impacto()).isEqualTo("Pode indicar degradação do revestimento.");
        assertThat(achado.gravidadeNormalizada()).isEqualTo(AnaliseVistoria.GravidadeAchado.ALTA);
        assertThat(achado.confiancaNormalizada()).isEqualTo(AnaliseVistoria.ConfiancaAchado.ALTA);
        assertThat(achado.recomendacao()).isEqualTo("Solicitar avaliação da origem da umidade.");
    }

    @Test
    void projetaContratoV1SemInventarMetadadosV2() {
        AnaliseVistoria.ImagemAnalise imagem = new AnaliseVistoria.ImagemAnalise(
                31L,
                "ana-legada",
                "Resumo legado",
                List.of("Sem medição instrumental."),
                new AnaliseVistoria.QualidadeImagem(true, List.of()),
                List.of());

        AnaliseVistoria analise = new AnaliseVistoria(1, List.of(imagem));

        assertThat(analise.version()).isEqualTo(1);
        assertThat(analise.execucao()).isNull();
        assertThat(analise.ambientes()).isEmpty();
        assertThat(analise.resultadoGeral()).isNull();
        assertThat(analise.motivoResultadoGeral()).isNull();
        assertThat(imagem.storagePath()).isNull();
        assertThat(imagem.ambiente()).isNull();
        assertThat(imagem.orientacaoNovaCaptura()).isNull();
        assertThat(imagem.qualidade().nivel()).isNull();
    }

    @Test
    void preservaMetadadosV2EProtegeListasContraMutacao() {
        var limitacoes = new ArrayList<>(List.of("Sem medição de umidade."));
        var imagem = new AnaliseVistoria.ImagemAnalise(
                31L,
                "vistorias/7/banheiro.jpg",
                "ana-7-img-31",
                new AnaliseVistoria.AmbienteImagem(8L, "Banheiro", CategoriaEvidencia.VISAO_GERAL),
                "Indício visual relevante.",
                limitacoes,
                null,
                new AnaliseVistoria.QualidadeImagem(
                        AnaliseVistoria.QualidadeEvidencia.SUFICIENTE, List.of()),
                List.of());
        var execucao = new AnaliseVistoria.ExecucaoAnalise(
                "oci", "google.gemini-2.5-flash", "vistoria-visual-v2", "ana-7",
                Instant.parse("2026-09-30T20:00:00Z"));
        var ambiente = new AnaliseVistoria.AmbienteResultado(
                8L, "Banheiro", AnaliseVistoria.ResultadoAnalise.NAO_APROVADO,
                "Foi identificado achado de alta gravidade.");

        AnaliseVistoria analise = new AnaliseVistoria(
                2, execucao, List.of(imagem), List.of(ambiente),
                AnaliseVistoria.ResultadoAnalise.NAO_APROVADO,
                "Ao menos um ambiente possui achado de alta gravidade.");
        limitacoes.clear();

        assertThat(analise.execucao().provider()).isEqualTo("oci");
        assertThat(analise.execucao().modelo()).isEqualTo("google.gemini-2.5-flash");
        assertThat(analise.imagens().getFirst().ambiente().id()).isEqualTo(8L);
        assertThat(analise.imagens().getFirst().ambiente().categoria())
                .isEqualTo(CategoriaEvidencia.VISAO_GERAL);
        assertThat(analise.imagens().getFirst().limitacoes()).containsExactly("Sem medição de umidade.");
        assertThat(analise.ambientes().getFirst().resultado())
                .isEqualTo(AnaliseVistoria.ResultadoAnalise.NAO_APROVADO);
        assertThatThrownBy(() -> analise.imagens().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
