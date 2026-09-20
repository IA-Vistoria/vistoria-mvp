package br.com.vistoriapredial.integration.vlm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class VlmIntegrationServiceTest {

    @Mock
    private StorageService storageService;

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder().baseUrl("http://vlm.test");
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void analyzesImageAndFormatsPreLaudo() {
        when(storageService.load(eq("uploads/foto.jpg"))).thenReturn(
                new StoredFile(new ByteArrayResource(new byte[] {1, 2, 3}), MediaType.IMAGE_JPEG, 3));

        server.expect(requestTo("http://vlm.test/health"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"status\":\"ok\",\"model_loaded\":true,\"model_id\":\"Qwen\",\"device\":\"cuda\"}",
                        MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://vlm.test/analyze"))
                .andExpect(method(POST))
                .andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess(
                        """
                        {
                          "analysis_id":"a1",
                          "image_quality":{"usable":true,"issues":[]},
                          "areas":[{
                            "area":"parede",
                            "issue_type":"possible_moisture",
                            "description":"Mancha aparente.",
                            "evidence":"Alteração de cor.",
                            "severity":"media",
                            "confidence":"media",
                            "recommendation":"Avaliação presencial.",
                            "location":null
                          }],
                          "overall_summary":"Há indícios visuais.",
                          "limitations":["Análise baseada apenas em imagem."]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        VlmIntegrationService service = new VlmIntegrationService(
                storageService, restClientBuilder.build(), "test-key");

        String preLaudo = service.analisarImagens(List.of("uploads/foto.jpg"));

        assertTrue(preLaudo.contains("Há indícios visuais."));
        assertTrue(preLaudo.contains("possible_moisture"));
        assertTrue(preLaudo.contains("uploads/foto.jpg"));
        server.verify();
    }

    @Test
    void failsWhenModelNotReady() {
        server.expect(requestTo("http://vlm.test/health"))
                .andExpect(method(GET))
                .andRespond(withStatus(SERVICE_UNAVAILABLE).body(
                        "{\"status\":\"model_not_ready\",\"model_loaded\":false}").contentType(MediaType.APPLICATION_JSON));

        VlmIntegrationService service = new VlmIntegrationService(
                storageService, restClientBuilder.build(), "test-key");

        assertThrows(IllegalStateException.class, () -> service.analisarImagens(List.of("uploads/foto.jpg")));
        server.verify();
    }
}
