package com.javadiscord.javabot.external_apis.github;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class File {
    private String filename;
    private String language;
    private String raw_url;
    private Integer size;
    private String content;
}
