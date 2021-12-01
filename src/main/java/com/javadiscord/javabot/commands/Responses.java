package com.javadiscord.javabot.commands;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;

/**
 * Utility class that provides standardized formatting for responses the bot
 * sends as replies to slash command events.
 */
public final class Responses {
	public static ReplyAction success(SlashCommandEvent event, String title, String message) {
		return reply(event, title, message, Bot.config.get(event.getGuild()).getSlashCommand().getInfoColor(), true);
	}

	public static WebhookMessageAction<Message> success(InteractionHook hook, String title, String message) {
		return reply(hook, title, message, Bot.config.get(hook.getInteraction().getGuild()).getSlashCommand().getSuccessColor(), true);
	}

	public static ReplyAction info(SlashCommandEvent event, String title, String message) {
		return reply(event, title, message, Bot.config.get(event.getGuild()).getSlashCommand().getInfoColor(), true);
	}

	public static WebhookMessageAction<Message> info(InteractionHook hook, String title, String message) {
		return reply(hook, title, message, Bot.config.get(hook.getInteraction().getGuild()).getSlashCommand().getInfoColor(), true);
	}

	public static ReplyAction error(SlashCommandEvent event, String message) {
		return reply(event, "An Error Occurred", message, Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor(), true);
	}

	public static WebhookMessageAction<Message> error(InteractionHook hook, String message) {
		return reply(hook, "An Error Occurred", message, Bot.config.get(hook.getInteraction().getGuild()).getSlashCommand().getErrorColor(), true);
	}

	public static ReplyAction warning(SlashCommandEvent event, String message) {
		return warning(event, null, message);
	}

	public static WebhookMessageAction<Message> warning(InteractionHook hook, String message) {
		return warning(hook, null, message);
	}

	public static ReplyAction warning(SlashCommandEvent event, String title, String message) {
		return reply(event, title, message, Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor(), true);
	}

	public static WebhookMessageAction<Message> warning(InteractionHook hook, String title, String message) {
		return reply(hook, title, message, Bot.config.get(hook.getInteraction().getGuild()).getSlashCommand().getWarningColor(), true);
	}

	/**
	 * Sends a reply to a slash command event.
	 * @param event The event to reply to.
	 * @param title The title of the reply message.
	 * @param message The message to send.
	 * @param color The color of the embed.
	 * @param ephemeral Whether the message should be ephemeral.
	 * @return The reply action.
	 */
	private static ReplyAction reply(SlashCommandEvent event, @Nullable String title, String message, Color color, boolean ephemeral) {
		return event.replyEmbeds(buildEmbed(title, message, color)).setEphemeral(ephemeral);
	}

	/**
	 * Sends a reply to an interaction hook.
	 * @param hook The interaction hook to send a message to.
	 * @param title The title of the message.
	 * @param message The message to send.
	 * @param color The color of the embed.
	 * @param ephemeral Whether the message should be ephemeral.
	 * @return The webhook message action.
	 */
	private static WebhookMessageAction<Message> reply(InteractionHook hook, @Nullable String title, String message, Color color, boolean ephemeral) {
		return hook.sendMessageEmbeds(buildEmbed(title, message, color)).setEphemeral(ephemeral);
	}

	private static MessageEmbed buildEmbed(@Nullable String title, String message, Color color) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTimestamp(Instant.now())
				.setColor(color);
		if (title != null && !title.isBlank()) {
			embedBuilder.setTitle(title);
		}
		embedBuilder.setDescription(message);
		return embedBuilder.build();
	}
}
