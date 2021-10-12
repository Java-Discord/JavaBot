package com.javadiscord.javabot.external_apis.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@ToString
public class File {
    private String filename;
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
}
