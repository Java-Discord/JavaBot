package net.discordjug.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.discordjug.javabot.data.config.GuildConfigItem;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

/**
 * Configuration for the guild's stats system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MetricsConfig extends GuildConfigItem {
	private long weeklyMessages = -1;
	private long activeMembers = -1;
	private long metricsCategoryId = 0;
	private String metricsMessageTemplate = "";

	public Category getMetricsCategory() {
		return getGuild().getCategoryById(this.metricsCategoryId);
	}
}
