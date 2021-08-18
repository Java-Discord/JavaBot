package com.javadiscord.javabot.properties.config;

import lombok.Data;

@Data
public class ModerationConfig {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long muteRoleId;
	private long staffRoleId;
}
