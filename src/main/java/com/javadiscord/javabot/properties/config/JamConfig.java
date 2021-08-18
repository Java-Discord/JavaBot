package com.javadiscord.javabot.properties.config;

import lombok.Data;

@Data
public class JamConfig {
	private long announcementChannelId;
	private long votingChannelId;

	private long pingRoleId;
	private long adminRoleId;
}
