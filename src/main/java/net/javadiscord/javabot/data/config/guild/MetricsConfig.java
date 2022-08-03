package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Category;
import net.javadiscord.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guild's stats system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MetricsConfig extends GuildConfigItem {
	private long weeklyMessages = 0;
	private long metricsCategoryId = 0;
	private String metricsMessageTemplate = "";

	public Category getMetricsCategory() {
		return getGuild().getCategoryById(this.metricsCategoryId);
	}
}
