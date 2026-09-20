package br.com.vistoriapredial.integration.vlm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VlmHealthResponse {

    private String status;

    @JsonProperty("model_loaded")
    private boolean modelLoaded;

    @JsonProperty("model_id")
    private String modelId;

    private String device;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public void setModelLoaded(boolean modelLoaded) {
        this.modelLoaded = modelLoaded;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isReady() {
        return "ok".equalsIgnoreCase(status) && modelLoaded;
    }
}
