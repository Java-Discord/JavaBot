package com.javadiscord.javabot.external_apis.github;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@NoArgsConstructor
@ToString
public class GistResponse {
    private String url;
    private Map<String, File> files;
}
