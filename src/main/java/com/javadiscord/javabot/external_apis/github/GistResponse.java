package com.javadiscord.javabot.external_apis.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@ToString
public class GistResponse {
    private String url;
    @JsonDeserialize(as = TreeMap.class)
    private Map<String, File> files;
}
