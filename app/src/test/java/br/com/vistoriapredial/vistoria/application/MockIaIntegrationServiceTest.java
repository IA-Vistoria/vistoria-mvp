package br.com.vistoriapredial.vistoria.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import org.junit.jupiter.api.Test;

class MockIaIntegrationServiceTest {

    private final MockIaIntegrationService service = new MockIaIntegrationService();

    @Test
    void shouldReturnStructuredJsonForImages() {
        String result = service.analisar(solicitacao(
                "http://test.com/img1.jpg", "http://test.com/img2.jpg"));

        assertTrue(result.contains("\"version\":1"));
        assertTrue(result.contains("http://test.com/img1.jpg"));
        assertTrue(result.contains("http://test.com/img2.jpg"));
        assertTrue(result.contains("overallSummary"));
    }

    @Test
    void shouldRejectNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.analisar(null));
    }

    @Test
    void shouldSimulateFailureForMagicUrl() {
        assertThrows(RuntimeException.class, () ->
            service.analisar(solicitacao("http://test.com/trigger-fail.jpg"))
        );
    }

    private SolicitacaoAnaliseIa solicitacao(String... paths) {
        List<EvidenciaAnaliseIa> evidencias = java.util.stream.IntStream.range(0, paths.length)
                .mapToObj(index -> new EvidenciaAnaliseIa(
                        (long) index + 1,
                        10L,
                        "Sala",
                        CategoriaEvidencia.VISAO_GERAL,
                        paths[index],
                        "image/jpeg"))
                .toList();
        return new SolicitacaoAnaliseIa(7L, evidencias);
    }
}
