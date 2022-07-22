package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;

/**
 * Utility class that provides standardized formatting for responses the bot
 * sends as replies to slash command events.
 */
public final class Responses {

	private Responses() {
	}

	public static @NotNull ReplyCallbackAction success(CommandInteraction event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.SUCCESS.getColor(), true);
	}

	public static @NotNull WebhookMessageAction<Message> success(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.SUCCESS.getColor(), true);
	}

	public static @NotNull ReplyCallbackAction info(CommandInteraction event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.INFO.getColor(), true);
	}

	public static @NotNull WebhookMessageAction<Message> info(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.INFO.getColor(), true);
	}

	public static @NotNull ReplyCallbackAction error(CommandInteraction event, String message, Object... args) {
		return reply(event, "An Error Occurred", String.format(message, args), Type.ERROR.getColor(), true);
	}

	public static @NotNull WebhookMessageAction<Message> error(InteractionHook hook, String message, Object... args) {
		return reply(hook, "An Error Occurred", String.format(message, args), Type.ERROR.getColor(), true);
	}

	public static @NotNull ReplyCallbackAction warning(CommandInteraction event, String message, Object... args) {
		return warning(event, null, String.format(message, args));
	}

	public static @NotNull WebhookMessageAction<Message> warning(InteractionHook hook, String message, Object... args) {
		return warning(hook, null, String.format(message, args));
	}

	public static @NotNull ReplyCallbackAction warning(CommandInteraction event, String title, String message, Object... args) {
		return reply(event, title, String.format(message, args), Type.WARN.getColor(), true);
	}

	public static @NotNull WebhookMessageAction<Message> warning(InteractionHook hook, String title, String message, Object... args) {
		return reply(hook, title, String.format(message, args), Type.WARN.getColor(), true);
	}

	public static @NotNull ReplyCallbackAction missingArguments(CommandInteraction event) {
		return error(event, "Missing required arguments.");
	}

	public static @NotNull WebhookMessageAction<Message> missingArguments(InteractionHook hook) {
		return error(hook, "Missing required arguments.");
	}

	public static @NotNull ReplyCallbackAction guildOnly(CommandInteraction event) {
		return error(event, "This command may only be used inside servers.");
	}

	public static @NotNull WebhookMessageAction<Message> guildOnly(InteractionHook hook) {
		return error(hook, "This command may only be used inside servers.");
	}

	public static @NotNull ReplyCallbackAction insufficientPermissions(CommandInteraction event) {
		return error(event, "I am missing one or more permissions in order to execute this action.");
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
	private static @NotNull ReplyCallbackAction reply(@NotNull CommandInteraction event, @Nullable String title, String message, Color color, boolean ephemeral) {
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
	private static @NotNull WebhookMessageAction<Message> reply(@NotNull InteractionHook hook, @Nullable String title, String message, Color color, boolean ephemeral) {
		return hook.sendMessageEmbeds(buildEmbed(title, message, color)).setEphemeral(ephemeral);
	}

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
