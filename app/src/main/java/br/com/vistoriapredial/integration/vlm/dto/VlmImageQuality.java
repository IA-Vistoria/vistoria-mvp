package br.com.vistoriapredial.integration.vlm.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VlmImageQuality {

    private boolean usable;
    private List<String> issues = new ArrayList<>();

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues != null ? issues : new ArrayList<>();
    }
}
