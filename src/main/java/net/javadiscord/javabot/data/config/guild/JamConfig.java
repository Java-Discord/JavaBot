package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.NewsChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.awt.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class JamConfig extends GuildConfigItem {
	private long announcementChannelId;
	private long votingChannelId;

	private long pingRoleId;
	private long adminRoleId;

	private String jamEmbedColorHex = "#FC5A03";

	public Color getJamEmbedColor() {
		return Color.decode(this.jamEmbedColorHex);
	}

	public NewsChannel getAnnouncementChannel() {
		return this.getGuild().getNewsChannelById(this.announcementChannelId);
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
