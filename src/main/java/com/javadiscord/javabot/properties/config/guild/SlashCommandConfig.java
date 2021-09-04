package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SlashCommandConfig extends GuildConfigItem {
	private String warningColor = "#eba434";
	private String errorColor = "#eb3434";
	private String infoColor = "#34a2eb";
	private String successColor = "#49de62";
}
