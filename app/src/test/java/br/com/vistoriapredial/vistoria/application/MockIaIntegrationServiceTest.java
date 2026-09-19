package br.com.vistoriapredial.vistoria.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockIaIntegrationServiceTest {

    private final MockIaIntegrationService service = new MockIaIntegrationService();

    @Test
    void shouldReturnMockPreLaudoWhenValidImagesProvided() {
        String result = service.analisarImagens(List.of("http://test.com/img1.jpg", "http://test.com/img2.jpg"));
        
        assertNotNull(result);
        assertTrue(result.contains("Foram analisadas 2 imagens"));
    }

    @Test
    void shouldThrowExceptionWhenEmptyImages() {
        assertThrows(IllegalArgumentException.class, () -> service.analisarImagens(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.analisarImagens(null));
    }

    @Test
    void shouldThrowExceptionWhenTriggerFailUrlProvided() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.analisarImagens(List.of("http://test.com/trigger-fail.jpg"))
        );
        assertEquals("Simulated AI Failure", ex.getMessage());
    }
}
