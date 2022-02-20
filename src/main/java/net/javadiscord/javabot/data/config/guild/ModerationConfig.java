package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long jobChannelId;
	private long staffRoleId;
	private long adminRoleId;

	/**
	 * ID of the share-knowledge channel.
	 */
	private long shareKnowledgeChannelId;

	/**
	 * The threshold for deleting a message in #share-knowledge. Note that this should be strictly < 0.
	 */
	private int shareKnowledgeMessageDeleteThreshold;

	/**
	 * The threshold for deleting a message in #looking-for-programmer.
	 */
	private int jobChannelMessageDeleteThreshold = 5;

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

	/**
	 * Invite liks AutoMod should exclude.
	 */
	private String[] automodInviteExcludes;

	/**
	 * Text that is sent to users when they're banned.
	 */
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

	public TextChannel getJobChannel() {
		return this.getGuild().getTextChannelById(this.jobChannelId);
	}

	public TextChannel getShareKnowledgeChannel() {
		return this.getGuild().getTextChannelById(this.shareKnowledgeChannelId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}

	public Role getAdminRole() {
		return this.getGuild().getRoleById(this.adminRoleId);
	}
}
