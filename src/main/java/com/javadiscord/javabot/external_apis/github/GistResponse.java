package com.javadiscord.javabot.external_apis.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GistResponse {
    private String url;
    private Map<String, File> files;

    @Override
    public String toString() {
        return "GistResponse{" +
                "url='" + url + '\'' +
                ", files=" + files +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public void setFiles(Map<String, File> files) {
        this.files = files;
    }
}
