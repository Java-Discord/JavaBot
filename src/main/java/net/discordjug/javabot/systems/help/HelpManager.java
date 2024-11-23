package net.discordjug.javabot.systems.help;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.HelpConfig;
import net.discordjug.javabot.data.h2db.DbActions;
import net.discordjug.javabot.systems.help.dao.HelpAccountRepository;
import net.discordjug.javabot.systems.help.dao.HelpTransactionRepository;
import net.discordjug.javabot.systems.user_preferences.UserPreferenceService;
import net.discordjug.javabot.systems.user_preferences.model.Preference;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.MessageActionUtils;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages all interactions regarding the help forum system.
 */
@RequiredArgsConstructor
public class HelpManager {
	/**
	 * The identifier used for all help thanks-related buttons.
	 */
	public static final String HELP_THANKS_IDENTIFIER = "forum-help-thank";

	/**
	 * The identifier used for all help close-related buttons.
	 */
	public static final String HELP_CLOSE_IDENTIFIER = "forum-help-close";

	/**
	 * The identifier used for all help guidelines-related buttons.
	 */
	public static final String HELP_GUIDELINES_IDENTIFIER = "forum-help-guidelines";

	@Getter
	private final ThreadChannel postThread;
	private final DbActions dbActions;
	private final BotConfig botConfig;
	private final HelpAccountRepository helpAccountRepository;

	private final HelpTransactionRepository helpTransactionRepository;
	private final UserPreferenceService preferenceService;

	/**
	 * Builds and replies {@link ActionRow}s with all members which helped the
	 * owner of the {@link HelpManager#postThread} forum post.
	 *
	 * @param callback The callback to reply to.
	 * @param helpers  The list of helpers to thank.
	 * @return The {@link ReplyCallbackAction}.
	 */
	public ReplyCallbackAction replyHelpThanks(IReplyCallback callback, @NotNull List<Member> helpers) {
		HelpConfig config = botConfig.get(callback.getGuild()).getHelpConfig();
		List<ItemComponent> helperThanksButtons = new ArrayList<>(20);
		for (Member helper : helpers.subList(0, Math.min(helpers.size(), 20))) {
			helperThanksButtons.add(Button.success(ComponentIdBuilder.build(HELP_THANKS_IDENTIFIER, postThread.getId(), helper.getId()), helper.getEffectiveName())
					.withEmoji(Emoji.fromUnicode("❤"))
			);
		}
		ActionRow controlsRow = ActionRow.of(
				Button.primary(ComponentIdBuilder.build(HELP_THANKS_IDENTIFIER, postThread.getId(), "done"), "I'm done here. Close this post!"),
				Button.danger(ComponentIdBuilder.build(HELP_THANKS_IDENTIFIER, postThread.getId(), "cancel"), "Cancel Closing")
		);
		List<ActionRow> rows = new ArrayList<>();
		rows.add(controlsRow);
		rows.addAll(MessageActionUtils.toActionRows(helperThanksButtons));
		return callback.reply(config.getHelpThanksMessageTemplate())
				.setComponents(rows);
	}

	/**
	 * Closes the {@link HelpManager#postThread}.
	 *
	 * @param callback    The callback to reply to.
	 * @param withHelpers Whether the help-thanks message should be displayed.
	 * @param reason      The reason for closing this post.
	 */
	public void close(IReplyCallback callback, boolean withHelpers, @Nullable String reason) {
		List<Member> helpers = getPostHelpers();
		if (withHelpers && !helpers.isEmpty()) {
			replyHelpThanks(callback, helpers).queue();
			return;
		}
		Responses.info(callback, "Post Closed", "This post has been closed by %s%s", callback.getUser().getAsMention(), reason != null ? " for the following reason:\n> " + reason : ".")
				.setEphemeral(false)
				.queue(s -> postThread.getManager().setLocked(true).setArchived(true).queue());
		if (callback.getMember().getIdLong() != postThread.getOwnerIdLong() &&
				Boolean.parseBoolean(preferenceService.getOrCreate(postThread.getOwnerIdLong(), Preference.PRIVATE_CLOSE_NOTIFICATIONS).getState())) {
			postThread
				.getJDA()
				.openPrivateChannelById(postThread.getOwnerIdLong())
				.flatMap(c -> createDMCloseInfoEmbed(callback.getMember(), postThread, reason, c))
				.queue(success -> {}, failure -> {});
			
			botConfig.get(callback.getGuild())
				.getModerationConfig()
				.getLogChannel()
				.sendMessageEmbeds(new EmbedBuilder()
						.setTitle("Post closed by non-original poster")
						.setDescription("The post " + postThread.getAsMention() +
								" has been closed by " + callback.getMember().getAsMention() + ".\n\n" +
								"[Post link](" + postThread.getJumpUrl() + ")")
						.addField("Reason", reason, false)
						.setAuthor(callback.getMember().getEffectiveName(), null, callback.getMember().getEffectiveAvatarUrl())
						.build())
				.queue();
		}
	}
	
	private RestAction<Message> createDMCloseInfoEmbed(Member closingMember, ThreadChannel postThread, String reason, PrivateChannel c) {
		EmbedBuilder eb = new EmbedBuilder()
			.setTitle("Post closed")
			.setDescription(
				"""
				Your post %s has been closed by %s
				
				[Post link](%s)
				"""
				.formatted(postThread.getAsMention(), closingMember.getAsMention(), postThread.getJumpUrl()))
			.addField("Deactivate", "You can deactivate notifications like this using the `/preferences` command.", false)
			.setAuthor(closingMember.getEffectiveName(), null, closingMember.getEffectiveAvatarUrl());
		if (reason != null) {
			eb
				.addField("Reason", reason, false);
		}
		return c.sendMessageEmbeds(eb.build());
	}

	/**
	 * Closes the {@link HelpManager#postThread}.
	 *
	 * @param closedBy    The {@link UserSnowflake} that closed this post.
	 * @param reason      The reason for closing this post.
	 */
	public void close(UserSnowflake closedBy, @Nullable String reason) {
		postThread.sendMessageEmbeds(buildPostClosedEmbed(closedBy, reason))
				.queue(s -> postThread.getManager().setArchived(true).queue());
	}
	
	/**
	 * Thanks a single user.
	 *
	 * @param guild The {@link Guild} the helper is thanked in
	 * @param postThread The {@link ThreadChannel} post.
	 * @param helperId   The helpers' discord id.
	 */
	public void thankHelper(@NotNull Guild guild, ThreadChannel postThread, long helperId) {
		guild.getJDA().retrieveUserById(helperId).queue(helper -> {
			// First insert the new thanks data.
			try {
				dbActions.update(
						"INSERT INTO help_channel_thanks (reservation_id, user_id, channel_id, helper_id) VALUES (?, ?, ?, ?)",
						postThread.getIdLong(),
						postThread.getOwnerIdLong(),
						postThread.getIdLong(),
						helper.getIdLong()
				);
				HelpConfig config = botConfig.get(guild).getHelpConfig();
				HelpExperienceService service = new HelpExperienceService(botConfig, helpAccountRepository, helpTransactionRepository);
				// Perform experience transactions
				service.performTransaction(helper.getIdLong(), config.getThankedExperience(), guild, postThread.getIdLong());
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				guild.getJDA().retrieveUserById(postThread.getOwnerIdLong()).queue(owner -> {
					botConfig.get(guild).getModerationConfig().getLogChannel().sendMessageFormat(
							"Could not record user %s thanking %s for help in post %s: %s",
							UserUtils.getUserTag(owner),
							UserUtils.getUserTag(helper),
							postThread.getAsMention(),
							e.getMessage()
							).queue();
				});
			}
		});
	}

	/**
	 * Checks if the interactions' user is eligible to close a forum post.
	 *
	 * @param interaction The interaction itself.
	 * @return Whether the user is eligible to close the post.
	 */
	public boolean isForumEligibleToBeUnreserved(@NotNull Interaction interaction) {
		if (interaction.getGuild() == null) return false;
		return interaction.getUser().getIdLong() == postThread.getOwnerIdLong() ||
				hasMemberHelperRole(interaction.getGuild(), interaction.getMember()) || hasMemberStaffRole(interaction.getGuild(), interaction.getMember());
	}

	private @NotNull MessageEmbed buildPostClosedEmbed(@NotNull UserSnowflake closedBy, @Nullable String reason) {
		return new EmbedBuilder()
				.setTitle("Post Closed")
				.setDescription("This post has been closed by %s%s".formatted(closedBy.getAsMention(), reason != null ? " for the following reason:\n> " + reason : "."))
				.setColor(Responses.Type.INFO.getColor())
				.build();
	}

	private boolean hasMemberStaffRole(@NotNull Guild guild, @Nullable Member member) {
		return member != null && member.getRoles().contains(botConfig.get(guild).getModerationConfig().getStaffRole());
	}

	private boolean hasMemberHelperRole(@NotNull Guild guild, @Nullable Member member) {
		return member != null && member.getRoles().contains(botConfig.get(guild).getHelpConfig().getHelperRole());
	}

	private @NotNull List<Member> getPostHelpers() {
		List<Message> messages = HelpListener.HELP_POST_MESSAGES.get(postThread.getIdLong());
		if (messages == null) return List.of();
		return messages.stream()
				.filter(m -> m.getMember() != null && m.getAuthor().getIdLong() != postThread.getOwnerIdLong())
				.map(Message::getMember)
				.distinct()
				.toList();
	}

	/**
	 * Calculates the experience for each user, based on the messages they sent.
	 *
	 * @param messages The list of {@link Message}s.
	 * @param ownerId The owner id.
	 * @param config The {@link HelpConfig}, containing some static info for the calculation.
	 * @return A {@link Map}, containing the users' id as the key, and the amount of xp as the value.
	 */
	public static Map<Long, Double> calculateExperience(List<Message> messages, long ownerId, HelpConfig config) {
		Map<Long, Double> experience = new HashMap<>();
		if (messages == null || messages.isEmpty()) return Map.of();
		for (User user : messages.stream().map(Message::getAuthor).collect(Collectors.toSet())) {
			if (user.getIdLong() == ownerId) continue;
			double xp = 0;
			for (Message message : messages.stream()
					.filter(f -> f.getAuthor().getIdLong() != ownerId && f.getContentDisplay().length() > config.getMinimumMessageLength()).toList()) {
				xp += config.getBaseExperience() + config.getPerCharacterExperience() * (Math.log(message.getContentDisplay().trim().length()) / Math.log(2));
			}
			experience.put(user.getIdLong(), Math.min(xp, config.getMaxExperiencePerChannel()));
		}
		return experience;
	}
}
