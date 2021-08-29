package com.javadiscord.javabot.properties.config;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
public class JamConfig {
	private long announcementChannelId;
	private long votingChannelId;

	private long pingRoleId;
	private long adminRoleId;

	private String jamEmbedColor = "#fc5a03";

	public TextChannel getAnnouncementChannel(Guild guild) {
		return guild.getTextChannelById(this.announcementChannelId);
	}

	public TextChannel getVotingChannel(Guild guild) {
		return guild.getTextChannelById(this.votingChannelId);
	}

	public Role getPingRole(Guild guild) {
		return guild.getRoleById(this.pingRoleId);
	}

	public Role getAdminRole(Guild guild) {
		return guild.getRoleById(this.adminRoleId);
	}
}
