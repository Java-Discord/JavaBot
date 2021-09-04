package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StatsConfig extends GuildConfigItem {
	private long categoryId;
	private String memberCountMessageTemplate;
}
