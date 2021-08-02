package com.javadiscord.javabot.commands;

import com.javadiscord.javabot.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.time.Instant;

/**
 * Utility class that provides standardized formatting for responses the bot
 * sends as replies to slash command events.
 */
public final class Responses {
	public static ReplyAction success(SlashCommandEvent event, String title, String message) {
		return reply(event, title, message, "responses.success", true);
	}

	public static ReplyAction info(SlashCommandEvent event, String title, String messsage) {
		return reply(event, title, messsage, "responses.info", true);
	}

	public static ReplyAction error(SlashCommandEvent event, String message) {
		return reply(event, "An Error Occurred", message, "responses.error", true);
	}

	public static ReplyAction warning(SlashCommandEvent event, String message) {
		return warning(event, null, message);
	}

	public static ReplyAction warning(SlashCommandEvent event, String title, String message) {
		return reply(event, title, message, "responses.warning", true);
	}

	private static ReplyAction reply(SlashCommandEvent event, String title, String message, String colorProperty, boolean ephemeral) {
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTimestamp(Instant.now())
				.setColor(Color.decode(Bot.getProperty(colorProperty)));
		if (title != null && !title.isBlank()) {
			embedBuilder.setTitle(title);
		}
		embedBuilder.setDescription(message);
		return event.replyEmbeds(embedBuilder.build()).setEphemeral(ephemeral);
	}
}
