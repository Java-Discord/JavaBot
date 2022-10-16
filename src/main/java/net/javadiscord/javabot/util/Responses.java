package net.javadiscord.javabot.util;

import java.awt.Color;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Utility class that provides standardized formatting for responses the bot
 * sends as replies to slash command events.
 */
public final class Responses {

	private Responses() {
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction success(IReplyCallback event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.SUCCESS.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> success(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.SUCCESS.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction info(IReplyCallback event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.INFO.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> info(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.INFO.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction error(IReplyCallback event, String message, Object... args) {
		return reply(event, "An Error Occurred", String.format(message, args), Type.ERROR.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> error(InteractionHook hook, String message, Object... args) {
		return reply(hook, "An Error Occurred", String.format(message, args), Type.ERROR.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction warning(IReplyCallback event, String message, Object... args) {
		return warning(event, null, String.format(message, args));
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> warning(InteractionHook hook, String message, Object... args) {
		return warning(hook, null, String.format(message, args));
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction warning(IReplyCallback event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.WARN.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> warning(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.WARN.getColor(), true);
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyMissingArguments(IReplyCallback event) {
		return error(event, "Missing required arguments.");
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> replyMissingArguments(InteractionHook hook) {
		return error(hook, "Missing required arguments.");
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyGuildOnly(IReplyCallback event) {
		return error(event, "This command may only be used inside servers.");
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> replyGuildOnly(InteractionHook hook) {
		return error(hook, "This command may only be used inside servers.");
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyInsufficientPermissions(IReplyCallback event, Permission... permissions) {
		return error(event, "I am missing one or more permissions in order to execute this action. (%s)",
				Arrays.stream(permissions).map(p -> MarkdownUtil.monospace(p.getName())).collect(Collectors.joining(", ")));
	}

	@CheckReturnValue
	public static @NotNull WebhookMessageCreateAction<Message> replyInsufficientPermissions(InteractionHook hook, Permission... permissions) {
		return error(hook, "I am missing one or more permissions in order to execute this action. (%s)",
				Arrays.stream(permissions).map(p -> MarkdownUtil.monospace(p.getName())).collect(Collectors.joining(", ")));
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyMissingMember(IReplyCallback event) {
		return error(event, "The provided user **must** be a member of this server. Please try again.");
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyCannotInteract(IReplyCallback event, @NotNull IMentionable mentionable) {
		return error(event, "I am missing permissions in order to interact with that. (%s)", mentionable.getAsMention());
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyStaffOnly(IReplyCallback event, GuildConfig guildConfig) {
		return error(event, "This command may only be used by staff members. (%s)", guildConfig.getModerationConfig().getStaffRole().getAsMention());
	}

	@CheckReturnValue
	public static @NotNull ReplyCallbackAction replyAdminOnly(IReplyCallback event, GuildConfig guildConfig) {
		return error(event, "This command may only be used by admins. (%s)", guildConfig.getModerationConfig().getAdminRole().getAsMention());
	}

	/**
	 * Sends a reply to a slash command event.
	 *
	 * @param event     The event to reply to.
	 * @param title     The title of the reply message.
	 * @param message   The message to send.
	 * @param color     The color of the embed.
	 * @param ephemeral Whether the message should be ephemeral.
	 * @return The reply action.
	 */
	@CheckReturnValue
	private static @NotNull ReplyCallbackAction reply(@NotNull IReplyCallback event, @Nullable String title, String message, Color color, boolean ephemeral) {
		return event.replyEmbeds(buildEmbed(title, message, color)).setEphemeral(ephemeral);
	}

	/**
	 * Sends a reply to an interaction hook.
	 *
	 * @param hook      The interaction hook to send a message to.
	 * @param title     The title of the message.
	 * @param message   The message to send.
	 * @param color     The color of the embed.
	 * @param ephemeral Whether the message should be ephemeral.
	 * @return The webhook message action.
	 */
	@CheckReturnValue
	private static @NotNull WebhookMessageCreateAction<Message> reply(@NotNull InteractionHook hook, @Nullable String title, String message, Color color, boolean ephemeral) {
		return hook.sendMessageEmbeds(buildEmbed(title, message, color)).setEphemeral(ephemeral);
	}

	@CheckReturnValue
	private static @NotNull MessageEmbed buildEmbed(@Nullable String title, String message, Color color) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTimestamp(Instant.now())
				.setColor(color);
		if (title != null && !title.isBlank()) {
			embedBuilder.setTitle(title);
		}
		embedBuilder.setDescription(message);
		return embedBuilder.build();
	}

	/**
	 * This enum contains all possible response types.
	 */
	public enum Type {
		/**
		 * The default response.
		 */
		DEFAULT(Color.decode("#2F3136")),
		/**
		 * An informing response.
		 */
		INFO(Color.decode("#34A2EB")),
		/**
		 * A successful response.
		 */
		SUCCESS(Color.decode("#49DE62")),
		/**
		 * A warning response.
		 */
		WARN(Color.decode("#EBA434")),
		/**
		 * An error response.
		 */
		ERROR(Color.decode("#EB3434"));

		private final Color color;

		Type(Color color) {
			this.color = color;
		}

		public Color getColor() {
			return this.color;
		}
	}
}
