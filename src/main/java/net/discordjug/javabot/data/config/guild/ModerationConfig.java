package net.discordjug.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.discordjug.javabot.data.config.GuildConfigItem;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.List;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long reportChannelId = 0;
	private long reportUserThreadHolderId = 0;
	private long applicationChannelId = 0;
	private long logChannelId = 0;
	private long suggestionChannelId = 0;
	private long jobChannelId = 0;
	private long projectChannelId = 0;
	private long staffRoleId = 0;
	private long adminRoleId = 0;
	private long expertRoleId = 0;

	/**
	 * ID of the share-knowledge channel.
	 */
	private long shareKnowledgeChannelId = 0;

	/**
	 * The threshold for deleting a message in #share-knowledge. Note that this should be strictly < 0.
	 */
	private int shareKnowledgeMessageDeleteThreshold;
	
	/**
	 * ID of the channel storing staff activity information.
	 */
	private long staffActivityChannelId = 0;

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
	 * The maximum total severity that a user can accrue from warnings before
	 * being timeouted in the server.
	 */
	private int timeoutSeverity = 50;

	/**
	 * The duration (in hours) to timeout users when they exceeded {@link #timeoutSeverity}.
	 */
	private int warnTimeoutHours = 2;
	
	/**
	 * ID of the channel where direct user notifications should be sent to (using private threads).
	 * @see net.discordjug.javabot.systems.notification.UserNotificationService
	 */
	private long notificationThreadChannelId;

	/**
	 * Invite links AutoMod should exclude.
	 */
	private List<String> automodInviteExcludes = List.of();

	/**
	 * Text that is sent to users when they're banned.
	 */
	private String banMessageText = "Looks like you've been banned from the Java Discord. If you want to appeal this decision please fill out our form at <https://airtable.com/shrp5V4H1U5TYOXyC>.";

	public TextChannel getReportChannel() {
		return this.getGuild().getTextChannelById(this.reportChannelId);
	}
	
	public TextChannel getReportUserThreadHolder() {
		return this.getGuild().getTextChannelById(this.reportUserThreadHolderId);
	}

	public TextChannel getApplicationChannel() {
		return this.getGuild().getTextChannelById(this.applicationChannelId);
	}

	public TextChannel getLogChannel() {
		return this.getGuild().getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel() {
		return this.getGuild().getTextChannelById(this.suggestionChannelId);
	}

	public ForumChannel getProjectChannel() {
		return this.getGuild().getForumChannelById(this.projectChannelId);
	}

	public ForumChannel getJobChannel() {
		return this.getGuild().getForumChannelById(this.jobChannelId);
	}
	
	public TextChannel getStaffActivityChannel() {
		return this.getGuild().getTextChannelById(this.staffActivityChannelId);
	}

	public ForumChannel getShareKnowledgeChannel() {
		return this.getGuild().getForumChannelById(this.shareKnowledgeChannelId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}

	public Role getAdminRole() {
		return this.getGuild().getRoleById(this.adminRoleId);
	}

	public Role getExpertRole() {
		return this.getGuild().getRoleById(this.expertRoleId);
	}

	public TextChannel getNotificationThreadChannel() {
		return this.getGuild().getTextChannelById(this.notificationThreadChannelId);
	}
}
