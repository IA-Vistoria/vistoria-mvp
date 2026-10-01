package br.com.vistoriapredial.integration.oci.genai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "oci.genai")
public class OciGenAiProperties {

    private AuthMode authMode = AuthMode.CONFIG_FILE;
    private String region = "sa-saopaulo-1";
    private String compartmentId;
    private String modelId = "google.gemini-2.5-flash";
    private String configFile = Path.of(
            System.getProperty("user.home"), ".oci", "config").toString();
    private String configProfile = "DEFAULT";
    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 30_000;
    private int maxAttempts = 3;
    private long retryDelayMs = 500;

    public void validarParaUso() {
        if (vazio(compartmentId)) {
            throw new IllegalStateException(
                    "OCI_COMPARTMENT_ID é obrigatório para o provider OCI.");
        }
        if (vazio(modelId)) {
            throw new IllegalStateException(
                    "OCI_GENAI_MODEL_ID é obrigatório para o provider OCI.");
        }
        if (vazio(region)) {
            throw new IllegalStateException("OCI_REGION é obrigatório para o provider OCI.");
        }
        if (authMode == null) {
            throw new IllegalStateException("OCI_AUTH_MODE é obrigatório para o provider OCI.");
        }
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalStateException("Os timeouts do OCI devem ser maiores que zero.");
        }
        if (maxAttempts < 1 || maxAttempts > 3 || retryDelayMs < 0) {
            throw new IllegalStateException(
                    "A política de retry do OCI deve usar entre 1 e 3 tentativas.");
        }
        if (authMode == AuthMode.CONFIG_FILE && (vazio(configFile) || vazio(configProfile))) {
            throw new IllegalStateException(
                    "O arquivo e o profile OCI são obrigatórios no modo config_file.");
        }
    }

    public String authModeLabel() {
        return authMode == AuthMode.CONFIG_FILE ? "config_file" : "instance_principal";
    }

    private boolean vazio(String value) {
        return value == null || value.isBlank();
    }

    public AuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(AuthMode authMode) {
        this.authMode = authMode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCompartmentId() {
        return compartmentId;
    }

    public void setCompartmentId(String compartmentId) {
        this.compartmentId = compartmentId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public String getConfigProfile() {
        return configProfile;
    }

    public void setConfigProfile(String configProfile) {
        this.configProfile = configProfile;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public enum AuthMode {
        CONFIG_FILE,
        INSTANCE_PRINCIPAL
    }
}
