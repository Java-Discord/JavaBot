package com.javadiscord.javabot.data.properties.config.guild;

import com.javadiscord.javabot.data.properties.config.GuildConfigItem;
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

	/**
	 * ID of the share-knowledge channel
	 */
	private long shareKnowledgeChannelId;

	/**
	 * The threshold for deleting a message in #share-knowledge. Note that this should be strictly < 0
	 */
	private int shareKnowledgeMessageDeleteThreshold;

	private int purgeMaxMessageCount = 1000;

	public TextChannel getReportChannel() {
		return this.getGuild().getTextChannelById(this.reportChannelId);
	}

	public TextChannel getLogChannel() {
		return this.getGuild().getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel() {
		return this.getGuild().getTextChannelById(this.suggestionChannelId);
	}

	public TextChannel getShareKnowledgeChannel() {
		return this.getGuild().getTextChannelById(this.shareKnowledgeChannelId);
	}

	public Role getMuteRole() {
		return this.getGuild().getRoleById(this.muteRoleId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}
}
