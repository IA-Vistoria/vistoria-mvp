package br.com.vistoriapredial.integration.oci.genai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health/ia")
@ConditionalOnBean(OciGenAiConfigCheck.class)
public class OciGenAiHealthController {

    private final OciGenAiConfigCheck configCheck;

    public OciGenAiHealthController(OciGenAiConfigCheck configCheck) {
        this.configCheck = configCheck;
    }

    @GetMapping
    public OciGenAiHealthResponse health() {
        OciGenAiConfigCheck.Resultado result = configCheck.verificar();
        return new OciGenAiHealthResponse(
                "oci",
                result.configurado(),
                result.autenticacaoDisponivel(),
                result.region(),
                result.modelo(),
                result.modoAutenticacao(),
                result.chamadaExternaRealizada());
    }

    public record OciGenAiHealthResponse(
            String provider,
            boolean configured,
            boolean authenticationAvailable,
            String region,
            String model,
            String authMode,
            boolean externalCallPerformed) {
    }
}
