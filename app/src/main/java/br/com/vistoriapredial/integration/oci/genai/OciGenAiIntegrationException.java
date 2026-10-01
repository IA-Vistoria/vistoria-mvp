package br.com.vistoriapredial.integration.oci.genai;

public class OciGenAiIntegrationException extends RuntimeException {

    private final OciGenAiFailureCategory categoria;

    public OciGenAiIntegrationException(
            OciGenAiFailureCategory categoria,
            String mensagem) {
        super(mensagem);
        this.categoria = categoria;
    }

    public OciGenAiIntegrationException(
            OciGenAiFailureCategory categoria,
            String mensagem,
            Throwable causa) {
        super(mensagem, causa);
        this.categoria = categoria;
    }

    public OciGenAiFailureCategory getCategoria() {
        return categoria;
    }
}
