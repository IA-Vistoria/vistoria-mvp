package br.com.vistoriapredial.integration.vlm.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VlmAnalyzeResponse {

    @JsonProperty("analysis_id")
    private String analysisId;

    @JsonProperty("image_quality")
    private VlmImageQuality imageQuality;

    private List<VlmAreaFinding> areas = new ArrayList<>();

    @JsonProperty("overall_summary")
    private String overallSummary;

    private List<String> limitations = new ArrayList<>();

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public VlmImageQuality getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(VlmImageQuality imageQuality) {
        this.imageQuality = imageQuality;
    }

    public List<VlmAreaFinding> getAreas() {
        return areas;
    }

    public void setAreas(List<VlmAreaFinding> areas) {
        this.areas = areas != null ? areas : new ArrayList<>();
    }

    public String getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(String overallSummary) {
        this.overallSummary = overallSummary;
    }

    public List<String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<String> limitations) {
        this.limitations = limitations != null ? limitations : new ArrayList<>();
    }
}
