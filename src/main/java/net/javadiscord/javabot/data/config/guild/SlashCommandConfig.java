package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.awt.*;

/**
 * Configuration for the bot's slash commands.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SlashCommandConfig extends GuildConfigItem {
	private String defaultColorHex = "#2F3136";
	private String warningColorHex = "#EBA434";
	private String errorColorHex = "#EB3434";
	private String infoColorHex = "#34A2EB";
	private String successColorHex = "#49DE62";

	public Color getDefaultColor() {
		return Color.decode(this.defaultColorHex);
	}

	public Color getWarningColor() {
		return Color.decode(this.warningColorHex);
	}

	public Color getErrorColor() {
		return Color.decode(this.errorColorHex);
	}

	public Color getInfoColor() {
		return Color.decode(this.infoColorHex);
	}

	public Color getSuccessColor() {
		return Color.decode(this.successColorHex);
	}
}
