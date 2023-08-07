package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

import java.util.Map;

/**
 * Configuration for the guilds' help system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HelpConfig extends GuildConfigItem {
	private long helpForumChannelId = 0;

	/**
	 * The id of the helper role.
	 */
	private long helperRoleId;

	/**
	 * The id of the help-ping role.
	 */
	private long helpNotificationChannelId;

	/**
	 * The message that's sent as soon as a user asks a question in an open help
	 * channel. This is only sent if it's not null.
	 */
	private String reservedChannelMessageTemplate = "`⌛` **This post has been reserved for your question.**\n> Hey %s! Please use `/close` or the `Close Post` button above when you're finished. Please remember to follow the help guidelines. This post will be automatically closed after %s minutes of inactivity.\n\n**TIP:** Narrow down your issue to __simple__ and __precise__ questions to maximize the chance that others will reply in here.";

	/**
	 * The message that's sent in a post to tell users that it
	 * is now marked as dormant and no more messages can be sent.
	 */
	private String dormantChannelMessageTemplate = "`\uD83D\uDCA4` **Post marked as dormant**\n> This post has been inactive for over %s minutes, thus, it has been **archived**.\n> If your question was not answered yet, feel free to re-open this post or create a new one.";

	/**
	 * The message that's sent in a post to tell users that it
	 * is now marked as dormant and no more messages can be sent.
	 */
	private String dormantChannelPrivateMessageTemplate = """ 
			Your post %s in %s has been inactive for over %s minutes, thus, it has been **archived**.
			If your question was not answered yet, feel free to re-open this post by sending another message in that channel.
			You can disable notifications like this using the `/preferences` command.
			[Post link](%s)
			""";

	/**
	 * The message that is sent in a post to tell users that they
	 * should use discord's code-formatting, provided the bot detects unformatted code.
	 * Issued by {@link net.javadiscord.javabot.systems.help.AutoCodeFormatter}
	 */
	private String formatHintMessage = "> Please format your code to make it more readable. \n> For java, it should look like this: \n```\u200B`\u200B`\u200B`\u200Bjava\npublic void foo() {\n \n}\u200B`\u200B`\u200B`\u200B```";

	/**
	 * The message that's sent when a user unreserved a channel where other users
	 * participated in.
	 */
	private String helpThanksMessageTemplate = "Before your post will be closed, would you like to express your gratitude to any of the people who helped you? When you're done, click **I'm done here. Close this post!**.";

	/**
	 * The number of minutes of inactivity before a channel is considered inactive.
	 */
	private int inactivityTimeoutMinutes = 300;

	/**
	 * The number of minutes to wait before closing a channel waiting for a response
	 * to a thanks question.
	 */
	private int removeThanksTimeoutMinutes = 10;

	/**
	 * How often users may use the /help-ping command.
	 */
	private int helpPingTimeoutSeconds = 300;

	/**
	 * The maximum amount of experience one can get from one help channel.
	 */
	private double maxExperiencePerChannel = 50;

	/**
	 * The base experience one gets for every message.
	 */
	private double baseExperience = 5;

	/**
	 * The weight for each character.
	 */
	private double perCharacterExperience = 1;

	/**
	 * The messages' minimum length.
	 */
	private int minimumMessageLength = 10;

	/**
	 * The message-embed's footnote of an unformatted-code-replacement.
	 * Issued by {@link net.javadiscord.javabot.systems.help.AutoCodeFormatter}
	 */
	private String autoformatInfoMessage = "This message has been formatted automatically.";

	/**
	 * The amount of experience points one gets for being thanked by the help channel owner.
	 */
	private double thankedExperience = 50;

	/**
	 * The amount of experience one gets for thanking other users.
	 */
	private double thankExperience = 3;

	/**
	 * The amount that should be subtracted from every Help Account each day.
	 */
	private double dailyExperienceSubtraction = 5;

	/**
	 * A list with all roles that are awarded by experience.
	 */
	private Map<Long, Double> experienceRoles = Map.of(0L, 0.0);

	public ForumChannel getHelpForumChannel() {
		return getGuild().getForumChannelById(helpForumChannelId);
	}

	public Role getHelperRole() {
		return getGuild().getRoleById(helperRoleId);
	}

	public TextChannel getHelpNotificationChannel() {
		return getGuild().getTextChannelById(helpNotificationChannelId);
	}
}
