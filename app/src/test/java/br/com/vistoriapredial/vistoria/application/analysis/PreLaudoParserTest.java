package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.exception.InvalidAiAnalysisException;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreLaudoParserTest {

    private final PreLaudoParser parser = new PreLaudoParser(new ObjectMapper());

    @Test
    void shouldProjectValidAnalysisUsingPublicImageId() {
        Vistoria vistoria = vistoriaWithImage(41L, "uploads/foto.jpg");
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
        assertThat(result.imagens()).hasSize(1);
        assertThat(result.imagens().getFirst().imagemId()).isEqualTo(41L);
        assertThat(result.imagens().getFirst().resumoGeral()).isEqualTo("Há uma marca próxima à janela.");
        assertThat(result.imagens().getFirst().qualidade().utilizavel()).isTrue();
        assertThat(result.imagens().getFirst().achados().getFirst().indice()).isZero();
        assertThat(result.imagens().getFirst().achados().getFirst().tipo()).isEqualTo("stain");
        assertThat(result.imagens().getFirst().achados().getFirst().localizacao()).isEqualTo("ao lado da janela");
    }

    @Test
    void shouldRejectMalformedJson() {
        Vistoria vistoria = vistoriaWithImage(41L, "uploads/foto.jpg");

        assertThatThrownBy(() -> parser.parse(vistoria, "{invalid"))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A análise da IA possui JSON inválido.");
    }

    @Test
    void shouldRejectUnsupportedVersion() {
        Vistoria vistoria = vistoriaWithImage(41L, "uploads/foto.jpg");

        assertThatThrownBy(() -> parser.parse(vistoria, "{\"version\":2,\"images\":[]}"))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A versão da análise da IA não é suportada.");
    }

    @Test
    void shouldRejectAnalysisForUnknownImage() {
        Vistoria vistoria = vistoriaWithImage(41L, "uploads/foto.jpg");
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
    void shouldRejectMoreThanOneHundredFindings() {
        Vistoria vistoria = vistoriaWithImage(41L, "uploads/foto.jpg");
        String findings = IntStream.range(0, 101)
                .mapToObj(index -> "{\"description\":\"achado " + index + "\"}")
                .collect(Collectors.joining(","));
        String raw = "{\"version\":1,\"images\":[{\"storagePath\":\"uploads/foto.jpg\"," +
                "\"imageQuality\":{\"usable\":true,\"issues\":[]},\"areas\":[" + findings + "]}]}";

        assertThatThrownBy(() -> parser.parse(vistoria, raw))
                .isInstanceOf(InvalidAiAnalysisException.class)
                .hasMessage("A análise da IA excede o limite de 100 achados.");
    }

    private Vistoria vistoriaWithImage(Long imageId, String storagePath) {
        Vistoria vistoria = new Vistoria();
        ImagemVistoria imagem = new ImagemVistoria();
        ReflectionTestUtils.setField(imagem, "id", imageId);
        imagem.setUrl(storagePath);
        imagem.setVistoria(vistoria);
        vistoria.getImagens().add(imagem);
        return vistoria;
    }
}
