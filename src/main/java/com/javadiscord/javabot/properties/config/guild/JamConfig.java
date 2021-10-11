package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class JamConfig extends GuildConfigItem {
	private long announcementChannelId;
	private long votingChannelId;

	private long pingRoleId;
	private long adminRoleId;

	private String jamEmbedColorHex = "#fc5a03";

	public Color getJamEmbedColor() { return Color.decode(this.jamEmbedColorHex); }

	public TextChannel getAnnouncementChannel() {
		return this.getGuild().getTextChannelById(this.announcementChannelId);
	}

	public TextChannel getVotingChannel() {
		return this.getGuild().getTextChannelById(this.votingChannelId);
	}

	public Role getPingRole() {
		return this.getGuild().getRoleById(this.pingRoleId);
	}

	public Role getAdminRole() {
		return this.getGuild().getRoleById(this.adminRoleId);
	}
}
