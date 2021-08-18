package com.javadiscord.javabot.properties.config;

import lombok.Data;

@Data
public class StatsConfig {
	private long channelId;
	private String memberCountMessageTemplate;
}
