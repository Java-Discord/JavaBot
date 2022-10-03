package net.javadiscord.javabot.systems.help.forum;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.config.guild.HelpForumConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpTransactionMessage;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Manages all interactions regarding the help forum system.
 */
@RequiredArgsConstructor
public class ForumHelpManager {
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
	private final DataSource dataSource;

	private final HelpAccountRepository helpAccountRepository;

	private final HelpTransactionRepository helpTransactionRepository;

	/**
	 * Builds and replies {@link ActionRow}s with all members which helped the
	 * owner of the {@link ForumHelpManager#postThread} forum post.
	 *
	 * @param callback The callback to reply to.
	 * @param helpers  The list of helpers to thank.
	 * @return The {@link ReplyCallbackAction}.
	 */
	public ReplyCallbackAction replyHelpThanks(IReplyCallback callback, @NotNull List<Member> helpers) {
		HelpForumConfig config = Bot.getConfig().get(callback.getGuild()).getHelpForumConfig();
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
		return callback.reply(config.getHelpThanksText())
				.setComponents(rows);
	}

	/**
	 * Closes the {@link ForumHelpManager#postThread}.
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
	}

	/**
	 * Thanks a single user.
	 *
	 * @param event      The {@link ButtonInteractionEvent} that was fired.
	 * @param postThread The {@link ThreadChannel} post.
	 * @param helperId   The helpers' discord id.
	 */
	public void thankHelper(@NotNull ButtonInteractionEvent event, ThreadChannel postThread, long helperId) {
		event.getJDA().retrieveUserById(helperId).queue(helper -> {
			// First insert the new thanks data.
			try {
				dbActions.update(
						"INSERT INTO help_channel_thanks (reservation_id, user_id, channel_id, helper_id) VALUES (?, ?, ?, ?)",
						postThread.getIdLong(),
						postThread.getOwnerIdLong(),
						postThread.getIdLong(),
						helper.getIdLong()
				);
				HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
				HelpExperienceService service = new HelpExperienceService(dataSource, botConfig, helpAccountRepository, helpTransactionRepository);
				// Perform experience transactions
				service.performTransaction(helper.getIdLong(), config.getThankedExperience(), HelpTransactionMessage.GOT_THANKED, event.getGuild());
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				botConfig.get(event.getGuild()).getModerationConfig().getLogChannel().sendMessageFormat(
						"Could not record user %s thanking %s for help in post %s: %s",
						postThread.getOwner().getUser().getAsTag(),
						helper.getAsTag(),
						postThread.getAsMention(),
						e.getMessage()
				).queue();
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

	private boolean hasMemberStaffRole(@NotNull Guild guild, @Nullable Member member) {
		return member != null && member.getRoles().contains(Bot.getConfig().get(guild).getModerationConfig().getStaffRole());
	}

	private boolean hasMemberHelperRole(@NotNull Guild guild, @Nullable Member member) {
		return member != null && member.getRoles().contains(Bot.getConfig().get(guild).getHelpConfig().getHelperRole());
	}

	private @NotNull List<Member> getPostHelpers() {
		List<Message> messages = ForumHelpListener.HELP_POST_MESSAGES.get(postThread.getIdLong());
		if (messages == null) return List.of();
		return messages.stream()
				.filter(m -> m.getMember() != null && m.getAuthor().getIdLong() != postThread.getOwnerIdLong())
				.map(Message::getMember)
				.distinct()
				.toList();
	}
}
