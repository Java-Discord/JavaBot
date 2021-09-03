package com.javadiscord.javabot.properties.config;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;

public abstract class GuildConfigItem {
	@Getter
	@Setter
	protected transient Guild guild;
}
