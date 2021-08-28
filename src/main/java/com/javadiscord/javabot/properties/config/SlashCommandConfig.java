package com.javadiscord.javabot.properties.config;

import lombok.Data;

@Data
public class SlashCommandConfig {
	private String warningColor = "#eba434";
	private String errorColor = "#eb3434";
	private String infoColor = "#34a2eb";
	private String successColor = "#49de62";
}
