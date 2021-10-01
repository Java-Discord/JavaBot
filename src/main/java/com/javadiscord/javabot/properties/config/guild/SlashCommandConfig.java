package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SlashCommandConfig extends GuildConfigItem {
	private String defaultColor = "#2F3136";
	private String warningColor = "#EBA434";
	private String errorColor = "#EB3434";
	private String infoColor = "#34A2EB";
	private String successColor = "#49DE62";
}
