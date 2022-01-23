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
public class StatsConfig extends GuildConfigItem {
	private long categoryId;
	private String memberCountMessageTemplate;

	public Category getCategory() {
		return getGuild().getCategoryById(this.categoryId);
	}
}
