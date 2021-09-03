package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long muteRoleId;
	private long staffRoleId;

	public TextChannel getReportChannel() {
		return guild.getTextChannelById(this.reportChannelId);
	}

	public TextChannel getLogChannel() {
		return guild.getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel() {
		return guild.getTextChannelById(this.suggestionChannelId);
	}

	public Role getMuteRole() {
		return guild.getRoleById(this.muteRoleId);
	}

	public Role getStaffRole() {
		return guild.getRoleById(this.staffRoleId);
	}
}
