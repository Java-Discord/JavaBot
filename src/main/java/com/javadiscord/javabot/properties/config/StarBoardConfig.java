package com.javadiscord.javabot.properties.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StarBoardConfig {
	private long channelId;
	private List<String> emotes = new ArrayList<>();
}
