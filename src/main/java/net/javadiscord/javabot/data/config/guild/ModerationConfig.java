package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long staffRoleId;

	/**
	 * ID of the share-knowledge channel.
	 */
	private long shareKnowledgeChannelId;

	/**
	 * The threshold for deleting a message in #share-knowledge. Note that this should be strictly < 0.
	 */
	private int shareKnowledgeMessageDeleteThreshold;

	private int purgeMaxMessageCount = 1000;

	/**
	 * The number of days for which a user's warning may contribute to them
	 * being removed from the server. Warnings older than this are still kept,
	 * but ignored.
	 */
	private int warnTimeoutDays = 30;

	/**
	 * The maximum total severity that a user can accrue from warnings before
	 * being removed from the server.
	 */
	private int maxWarnSeverity = 100;

	private String banMessageText = "Looks like you've been banned from the Java Discord. If you want to appeal this decision please fill out our form at <https://airtable.com/shrp5V4H1U5TYOXyC>.";

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

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}
}
