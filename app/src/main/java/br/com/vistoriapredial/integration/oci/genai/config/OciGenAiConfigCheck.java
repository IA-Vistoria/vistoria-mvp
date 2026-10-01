package br.com.vistoriapredial.integration.oci.genai.config;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;

public class OciGenAiConfigCheck {

    private final OciGenAiProperties properties;
    private final AbstractAuthenticationDetailsProvider authenticationDetailsProvider;

    public OciGenAiConfigCheck(
            OciGenAiProperties properties,
            AbstractAuthenticationDetailsProvider authenticationDetailsProvider) {
        this.properties = properties;
        this.authenticationDetailsProvider = authenticationDetailsProvider;
    }

    public Resultado verificar() {
        properties.validarParaUso();
        return new Resultado(
                true,
                properties.getRegion(),
                properties.getModelId(),
                properties.authModeLabel(),
                authenticationDetailsProvider != null,
                false);
    }

    public record Resultado(
            boolean configurado,
            String region,
            String modelo,
            String modoAutenticacao,
            boolean autenticacaoDisponivel,
            boolean chamadaExternaRealizada) {
    }
}
