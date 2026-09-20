package br.com.vistoriapredial.integration.vlm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vlm")
public class VlmProperties {

    private String url = "http://127.0.0.1:8001";
    private String apiKey = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 300_000;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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

    public String baseUrl() {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("VLM_URL / vlm.url não configurado.");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
