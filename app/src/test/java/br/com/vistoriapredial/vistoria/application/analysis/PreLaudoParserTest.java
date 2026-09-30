package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.exception.InvalidAiAnalysisException;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.TipoAmbiente;
import br.com.vistoriapredial.vistoria.domain.TipoImovel;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreLaudoParserTest {

    private final PreLaudoParser parser = new PreLaudoParser(new ObjectMapper());

    @Test
    void deveProjetarTodosOsCamposDeUmaAnaliseV2Valida() {
        AnaliseVistoria result = parser.parse(vistoriaV2(), analiseV2Valida());

        assertThat(result.version()).isEqualTo(2);
        assertThat(result.execucao()).isEqualTo(new AnaliseVistoria.ExecucaoAnalise(
                "oci", "google.gemini-2.5-flash", "vistoria-visual-v2", "ana-7",
                Instant.parse("2026-09-30T20:00:00Z")));
        assertThat(result.resultadoGeral()).isEqualTo(AnaliseVistoria.ResultadoAnalise.NAO_APROVADO);
        assertThat(result.motivoResultadoGeral())
                .isEqualTo("Ao menos um ambiente possui achado de alta ou crítica gravidade.");
        assertThat(result.ambientes()).containsExactly(new AnaliseVistoria.AmbienteResultado(
                8L, "Banheiro", AnaliseVistoria.ResultadoAnalise.NAO_APROVADO,
                "Foi identificado ao menos um achado de alta ou crítica gravidade."));

        AnaliseVistoria.ImagemAnalise imagem = result.imagens().getFirst();
        assertThat(imagem.imagemId()).isEqualTo(31L);
        assertThat(imagem.storagePath()).isEqualTo("uploads/banheiro.webp");
        assertThat(imagem.identificadorAnalise()).isEqualTo("ana-7");
        assertThat(imagem.ambiente()).isEqualTo(new AnaliseVistoria.AmbienteImagem(
                8L, "Banheiro", CategoriaEvidencia.VISAO_GERAL));
        assertThat(imagem.resumoGeral()).isEqualTo("Mofo aparente na parede do banheiro.");
        assertThat(imagem.limitacoes()).containsExactly("Sem medição de umidade.");
        assertThat(imagem.orientacaoNovaCaptura()).isNull();
        assertThat(imagem.qualidade().nivel()).isEqualTo(AnaliseVistoria.QualidadeEvidencia.SUFICIENTE);
        assertThat(imagem.qualidade().problemas()).isEmpty();

        AnaliseVistoria.AchadoIa achado = imagem.achados().getFirst();
        assertThat(achado.criterio()).isEqualTo("Integridade aparente da parede");
        assertThat(achado.area()).isEqualTo("parede");
        assertThat(achado.tipo()).isEqualTo("mofo_aparente");
        assertThat(achado.descricao()).isEqualTo("Manchas escuras e material pulverulento.");
        assertThat(achado.evidencia()).isEqualTo("Distribuição extensa na superfície visível.");
        assertThat(achado.impacto()).isEqualTo("Pode indicar umidade persistente.");
        assertThat(achado.gravidadeNormalizada()).isEqualTo(AnaliseVistoria.GravidadeAchado.ALTA);
        assertThat(achado.confiancaNormalizada()).isEqualTo(AnaliseVistoria.ConfiancaAchado.ALTA);
        assertThat(achado.recomendacao()).isEqualTo("Solicitar avaliação presencial da origem da umidade.");
        assertThat(achado.localizacao()).isEqualTo("parede ao lado da porta");
    }

    @Test
    void deveManterDocumentoV1Legivel() {
        Vistoria vistoria = vistoriaComImagemLegada(41L, "uploads/foto.jpg");
        String raw = """
                {
                  "version": 1,
                  "images": [{
                    "storagePath": "uploads/foto.jpg",
                    "analysisId": "ana-7",
                    "overallSummary": "Há uma marca próxima à janela.",
                    "limitations": ["Sem medição de umidade."],
                    "imageQuality": {"usable": true, "issues": []},
                    "areas": [{
                      "area": "parede",
                      "issueType": "stain",
                      "description": "Marca escura localizada.",
                      "evidence": "Contraste visual na pintura.",
                      "severity": "baixa",
                      "confidence": "média",
                      "recommendation": "Registrar evolução visual.",
                      "location": "ao lado da janela"
                    }]
                  }]
                }
                """;

        AnaliseVistoria result = parser.parse(vistoria, raw);

        assertThat(result.version()).isEqualTo(1);
        assertThat(result.execucao()).isNull();
        assertThat(result.imagens()).hasSize(1);
        assertThat(result.imagens().getFirst().imagemId()).isEqualTo(41L);
        assertThat(result.imagens().getFirst().achados().getFirst().tipo()).isEqualTo("stain");
        assertThat(result.ambientes()).isEmpty();
        assertThat(result.resultadoGeral()).isNull();
    }

    @Test
    void deveRejeitarJsonMalformado() {
        assertThatThrownBy(() -> parser.parse(vistoriaV2(), "{invalid"))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A análise da IA possui JSON inválido.");
    }

    @Test
    void deveRejeitarVersaoNaoSuportada() {
        assertThatThrownBy(() -> parser.parse(vistoriaV2(), "{\"version\":3,\"images\":[]}"))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A versão da análise da IA não é suportada.");
    }

    @Test
    void deveRejeitarCaminhoDeImagemDesconhecidoNoV2() {
        assertV2Invalido(analiseV2Valida().replace(
                "uploads/banheiro.webp", "uploads/desconhecida.webp"),
                "A análise da IA referencia uma evidência desconhecida.");
    }

    @Test
    void deveRejeitarIdDeImagemDivergenteNoV2() {
        assertV2Invalido(analiseV2Valida().replace("\"imageId\": 31", "\"imageId\": 99"),
                "A análise da IA referencia uma evidência desconhecida.");
    }

    @Test
    void deveRejeitarIdDeAmbienteDesconhecidoNoV2() {
        assertV2Invalido(analiseV2Valida().replace("\"id\": 8", "\"id\": 98"),
                "A análise da IA referencia um ambiente desconhecido.");
    }

    @Test
    void deveRejeitarNomeDeAmbienteDivergenteNoV2() {
        assertV2Invalido(analiseV2Valida().replace("\"name\": \"Banheiro\"", "\"name\": \"Cozinha\""),
                "A análise da IA referencia um ambiente desconhecido.");
    }

    @Test
    void deveRejeitarCategoriaInvalidaNoV2() {
        assertV2Invalido(analiseV2Valida().replace("VISAO_GERAL", "PANORAMA"),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarQualidadeInvalidaNoV2() {
        assertV2Invalido(analiseV2Valida().replace("SUFICIENTE", "REGULAR"),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarGravidadeInvalidaNoV2() {
        assertV2Invalido(analiseV2Valida().replace("\"severity\": \"ALTA\"", "\"severity\": \"URGENTE\""),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarConfiancaInvalidaNoV2() {
        assertV2Invalido(analiseV2Valida().replace("\"confidence\": \"ALTA\"", "\"confidence\": \"TOTAL\""),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarCampoObrigatorioVazioNoV2() {
        assertV2Invalido(analiseV2Valida().replace(
                        "\"criterion\": \"Integridade aparente da parede\"", "\"criterion\": \"   \""),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarTextoAcimaDeDoisMilCaracteresNoV2() {
        String textoExcedente = "x".repeat(2_001);
        assertV2Invalido(analiseV2Valida().replace(
                        "Manchas escuras e material pulverulento.", textoExcedente),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarResultadoDoAmbienteContraditorioNoV2() {
        assertV2Invalido(analiseV2Valida().replace(
                        "\"result\": \"NAO_APROVADO\"", "\"result\": \"APROVADO\""),
                "A análise da IA apresenta resultado incompatível com as evidências.");
    }

    @Test
    void deveRejeitarResultadoGeralContraditorioNoV2() {
        String raw = analiseV2Valida().replace(
                "\"overallResult\": \"NAO_APROVADO\"", "\"overallResult\": \"APROVADO\"");

        assertV2Invalido(raw, "A análise da IA apresenta resultado incompatível com as evidências.");
    }

    @Test
    void deveRejeitarInstanteDeExecucaoInvalidoNoV2() {
        assertV2Invalido(analiseV2Valida().replace(
                        "2026-09-30T20:00:00Z", "30/09/2026 20:00"),
                "A análise da IA não atende ao contrato esperado.");
    }

    @Test
    void deveRejeitarMaisDeCemAchadosNoV2() {
        String achado = """
                {"criterion":"Integridade","area":"parede","type":"mofo_aparente",
                 "description":"Marca.","evidence":"Contraste.","impact":"Possível umidade.",
                 "severity":"BAIXA","confidence":"MEDIA","recommendation":"Inspecionar.",
                 "location":"parede"}
                """;
        String achados = IntStream.range(0, 101).mapToObj(index -> achado)
                .collect(Collectors.joining(","));
        String raw = analiseV2Valida().replaceFirst(
                "(?s)\"findings\"\\s*:\\s*\\[.*?]\\s*}\\s*]",
                "\"findings\":[" + achados + "]}]" );

        assertV2Invalido(raw, "A análise da IA excede o limite de 100 achados.");
    }

    @Test
    void deveRejeitarImagemDesconhecidaNoV1() {
        Vistoria vistoria = vistoriaComImagemLegada(41L, "uploads/foto.jpg");
        String raw = """
                {"version":1,"images":[{
                  "storagePath":"uploads/outra.jpg",
                  "imageQuality":{"usable":true,"issues":[]},
                  "areas":[]
                }]}
                """;

        assertThatThrownBy(() -> parser.parse(vistoria, raw))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A análise da IA referencia uma evidência desconhecida.");
    }

    @Test
    void deveRejeitarMaisDeCemAchadosNoV1() {
        Vistoria vistoria = vistoriaComImagemLegada(41L, "uploads/foto.jpg");
        String findings = IntStream.range(0, 101)
                .mapToObj(index -> "{\"description\":\"achado " + index + "\"}")
                .collect(Collectors.joining(","));
        String raw = "{\"version\":1,\"images\":[{\"storagePath\":\"uploads/foto.jpg\"," +
                "\"imageQuality\":{\"usable\":true,\"issues\":[]},\"areas\":[" + findings + "]}]}";

        assertThatThrownBy(() -> parser.parse(vistoria, raw))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A análise da IA excede o limite de 100 achados.");
    }

    private void assertV2Invalido(String raw, String mensagem) {
        assertThatThrownBy(() -> parser.parse(vistoriaV2(), raw))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage(mensagem);
    }

    private Vistoria vistoriaV2() {
        Vistoria vistoria = new Vistoria();
        AmbienteVistoria ambiente = AmbienteVistoria.criar(TipoAmbiente.BANHEIRO, "Banheiro", 0);
        vistoria.configurarRoteiro(TipoImovel.APARTAMENTO, List.of(ambiente));
        ReflectionTestUtils.setField(ambiente, "id", 8L);
        ImagemVistoria imagem = new ImagemVistoria(
                vistoria, ambiente, CategoriaEvidencia.VISAO_GERAL,
                "uploads/banheiro.webp", LocalDateTime.of(2026, 9, 30, 19, 50));
        imagem.setId(31L);
        vistoria.getImagens().add(imagem);
        return vistoria;
    }

    private Vistoria vistoriaComImagemLegada(Long imageId, String storagePath) {
        Vistoria vistoria = new Vistoria();
        ImagemVistoria imagem = new ImagemVistoria();
        ReflectionTestUtils.setField(imagem, "id", imageId);
        imagem.setUrl(storagePath);
        imagem.setVistoria(vistoria);
        vistoria.getImagens().add(imagem);
        return vistoria;
    }

    private String analiseV2Valida() {
        return """
                {
                  "version": 2,
                  "execution": {
                    "provider": "oci",
                    "model": "google.gemini-2.5-flash",
                    "promptVersion": "vistoria-visual-v2",
                    "analysisId": "ana-7",
                    "completedAt": "2026-09-30T20:00:00Z"
                  },
                  "images": [{
                    "imageId": 31,
                    "storagePath": "uploads/banheiro.webp",
                    "environment": {"id": 8, "name": "Banheiro", "category": "VISAO_GERAL"},
                    "imageQuality": "SUFICIENTE",
                    "summary": "Mofo aparente na parede do banheiro.",
                    "limitations": ["Sem medição de umidade."],
                    "captureGuidance": null,
                    "findings": [{
                      "criterion": "Integridade aparente da parede",
                      "area": "parede",
                      "type": "mofo_aparente",
                      "description": "Manchas escuras e material pulverulento.",
                      "evidence": "Distribuição extensa na superfície visível.",
                      "impact": "Pode indicar umidade persistente.",
                      "severity": "ALTA",
                      "confidence": "ALTA",
                      "recommendation": "Solicitar avaliação presencial da origem da umidade.",
                      "location": "parede ao lado da porta"
                    }]
                  }],
                  "environments": [{
                    "id": 8,
                    "name": "Banheiro",
                    "result": "NAO_APROVADO",
                    "resultReason": "Informado pelo modelo e validado localmente."
                  }],
                  "overallResult": "NAO_APROVADO",
                  "overallReason": "Informado pelo modelo e validado localmente."
                }
                """;
    }
}
