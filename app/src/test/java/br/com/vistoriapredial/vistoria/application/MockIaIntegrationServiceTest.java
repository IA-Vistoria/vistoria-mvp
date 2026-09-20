package br.com.vistoriapredial.vistoria.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MockIaIntegrationServiceTest {

    private final MockIaIntegrationService service = new MockIaIntegrationService();

    @Test
    void shouldReturnStructuredJsonForImages() {
        String result = service.analisarImagens(List.of("http://test.com/img1.jpg", "http://test.com/img2.jpg"));

        assertTrue(result.contains("\"version\":1"));
        assertTrue(result.contains("http://test.com/img1.jpg"));
        assertTrue(result.contains("http://test.com/img2.jpg"));
        assertTrue(result.contains("overallSummary"));
    }

    @Test
    void shouldRejectEmptyImageList() {
        assertThrows(IllegalArgumentException.class, () -> service.analisarImagens(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.analisarImagens(null));
    }

    @Test
    void shouldSimulateFailureForMagicUrl() {
        assertThrows(RuntimeException.class, () ->
            service.analisarImagens(List.of("http://test.com/trigger-fail.jpg"))
        );
    }
}
