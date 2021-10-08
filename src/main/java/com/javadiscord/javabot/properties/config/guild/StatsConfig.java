package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Category;

import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatsConfig extends GuildConfigItem {
	private long categoryId;
	private String memberCountMessageTemplate;

	public Category getCategory() {
		return getGuild().getCategoryById(this.categoryId);
	}
}
