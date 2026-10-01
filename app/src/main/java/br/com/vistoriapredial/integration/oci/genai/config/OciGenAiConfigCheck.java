package br.com.vistoriapredial.integration.oci.genai.config;

public class OciGenAiConfigCheck {

    private final OciGenAiProperties properties;

    public OciGenAiConfigCheck(OciGenAiProperties properties) {
        this.properties = properties;
    }

    public Resultado verificar() {
        properties.validarParaUso();
        return new Resultado(
                true,
                properties.getRegion(),
                properties.getModelId(),
                properties.authModeLabel());
    }

    public record Resultado(
            boolean configurado,
            String region,
            String modelo,
            String modoAutenticacao) {
    }
}
