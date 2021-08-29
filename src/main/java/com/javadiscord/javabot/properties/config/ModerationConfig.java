package com.javadiscord.javabot.properties.config;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
public class ModerationConfig {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long muteRoleId;
	private long staffRoleId;

	public TextChannel getReportChannel(Guild guild) {
		return guild.getTextChannelById(this.reportChannelId);
	}

	public TextChannel getLogChannel(Guild guild) {
		return guild.getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel(Guild guild) {
		return guild.getTextChannelById(this.suggestionChannelId);
	}

	public Role getMuteRole(Guild guild) {
		return guild.getRoleById(this.muteRoleId);
	}

	public Role getStaffRole(Guild guild) {
		return guild.getRoleById(this.staffRoleId);
	}
}
