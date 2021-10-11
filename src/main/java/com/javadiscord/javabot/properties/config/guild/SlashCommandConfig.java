package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.awt.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class SlashCommandConfig extends GuildConfigItem {
	private String defaultColorHex = "#2F3136";
	private String warningColorHex = "#EBA434";
	private String errorColorHex = "#EB3434";
	private String infoColorHex = "#34A2EB";
	private String successColorHex = "#49DE62";

	public Color getDefaultColor() { return Color.decode(this.defaultColorHex); }

	public Color getWarningColor() { return Color.decode(this.warningColorHex); }

	public Color getErrorColor() { return Color.decode(this.errorColorHex); }

	public Color getInfoColor() { return Color.decode(this.infoColorHex); }

	public Color getSuccessColor() { return Color.decode(this.successColorHex); }
}
