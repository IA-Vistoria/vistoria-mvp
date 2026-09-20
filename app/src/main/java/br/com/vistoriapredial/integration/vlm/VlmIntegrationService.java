package br.com.vistoriapredial.integration.vlm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.com.vistoriapredial.integration.vlm.dto.VlmAnalyzeResponse;
import br.com.vistoriapredial.integration.vlm.dto.VlmHealthResponse;
import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.vistoria.application.IaIntegrationService;

@Service
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "vlm")
public class VlmIntegrationService implements IaIntegrationService {

    private final StorageService storageService;
    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public VlmIntegrationService(StorageService storageService, VlmProperties properties) {
        this.storageService = storageService;
        this.apiKey = properties.getApiKey();
        if (!StringUtils.hasText(this.apiKey)) {
            throw new IllegalStateException("VLM_API_KEY / vlm.api-key é obrigatório quando app.ia.provider=vlm.");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** Construtor de teste com RestClient injetável. */
    VlmIntegrationService(StorageService storageService, RestClient restClient, String apiKey) {
        this.storageService = storageService;
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public String analisarImagens(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem fornecida para análise.");
        }

        ensureModelReady();

        List<VlmPreLaudoFormatter.AnalyzedImage> analyzed = new ArrayList<>(imageUrls.size());
        for (String path : imageUrls) {
            analyzed.add(new VlmPreLaudoFormatter.AnalyzedImage(path, analyzeOne(path)));
        }
        return VlmPreLaudoFormatter.format(analyzed);
    }

    private void ensureModelReady() {
        try {
            VlmHealthResponse health = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(VlmHealthResponse.class);

            if (health == null || !health.isReady()) {
                throw new IllegalStateException("Serviço de análise indisponível: modelo não está pronto.");
            }
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Serviço de análise indisponível (HTTP " + exception.getStatusCode().value() + ").",
                    exception);
        }
    }

    private VlmAnalyzeResponse analyzeOne(String relativePath) {
        StoredFile stored = storageService.load(relativePath);
        byte[] bytes;
        try {
            bytes = stored.resource().getContentAsByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler evidência para análise: " + relativePath, exception);
        }

        String filename = fileNameFrom(relativePath);

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("image", new NamedByteArrayResource(bytes, filename));

        try {
            VlmAnalyzeResponse response = restClient.post()
                    .uri("/analyze")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(VlmAnalyzeResponse.class);

            if (response == null) {
                throw new IllegalStateException("Resposta vazia do serviço de análise.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Falha na análise VLM (HTTP " + exception.getStatusCode().value() + ").",
                    exception);
        }
    }

    private static String fileNameFrom(String relativePath) {
        int slash = Math.max(relativePath.lastIndexOf('/'), relativePath.lastIndexOf('\\'));
        return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
