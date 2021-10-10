package com.javadiscord.javabot.external_apis.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class File {
    private String language;
    private String raw_url;
    private Integer size;
    private String content;

    @Override
    public String toString() {
        return "File{" +
                "language='" + language + '\'' +
                ", rawURL='" + raw_url + '\'' +
                ", size=" + size +
                ", content='" + content + '\'' +
                '}';
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRaw_url() {
        return raw_url;
    }

    public void setRaw_url(String raw_url) {
        this.raw_url = raw_url;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
