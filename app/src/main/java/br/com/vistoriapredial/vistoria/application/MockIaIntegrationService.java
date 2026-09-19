package br.com.vistoriapredial.vistoria.application;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockIaIntegrationService implements IaIntegrationService {

    @Override
    public String analisarImagens(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem fornecida para análise.");
        }
        
        // Simulação de falha baseada em um magic word na URL
        if (imageUrls.stream().anyMatch(url -> url.contains("trigger-fail"))) {
            throw new RuntimeException("Simulated AI Failure");
        }
        
        return "Pré-laudo gerado pela IA (MOCK). Foram analisadas " + imageUrls.size() + " imagens.\n" +
               "- Infiltração detectada no teto.\n" +
               "- Piso em boas condições.";
    }
}
