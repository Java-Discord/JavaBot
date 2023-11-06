package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * This class represents the `/change-my-mind` command.
 */
public class ChangeMyMindCommand extends SlashCommand {
	/**
	 * The maximum acceptable length for texts to send to the API. Technically,
	 * the API supports up to but not including 2000, but we'll make that much
	 * lower to avoid issues, since the API fails with a 501 error. People
	 * shouldn't really be putting paragraphs into this anyway.
	 */
	private static final int MAX_SEARCH_TERM_LENGTH = 500;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public ChangeMyMindCommand() {
		setCommandData(Commands.slash("change-my-mind", "Generates the \"Change My Mind\" meme from your given input.")
				.addOption(OptionType.STRING, "text", "The text which should be used on the template.", true)
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		String encodedSearchTerm = URLEncoder.encode(Objects.requireNonNull(event.getOption("text")).getAsString(), StandardCharsets.UTF_8);
		if (encodedSearchTerm.length() > MAX_SEARCH_TERM_LENGTH) {
			event.reply("The text you provided is too long. It may not be more than " + MAX_SEARCH_TERM_LENGTH + " characters.").queue();
			return;
		}
		event.deferReply(false).queue();
		Unirest.get("https://nekobot.xyz/api/imagegen?type=changemymind&text=" + encodedSearchTerm).asJsonAsync(new Callback<>() {
			@Override
			public void completed(HttpResponse<JsonNode> hr) {
				try {
					String imageUrl = hr.getBody().getObject().getString("message");
					event.getHook().sendMessageEmbeds(new EmbedBuilder()
							.setAuthor(UserUtils.getUserTag(event.getUser()), imageUrl, event.getUser().getEffectiveAvatarUrl())
							.setColor(Responses.Type.DEFAULT.getColor())
							.setImage(imageUrl)
							.setTimestamp(Instant.now())
							.build()
					).queue();
				} catch (JSONException jsonException) {
					ExceptionLogger.capture(jsonException, getClass().getSimpleName());
					event.getHook().sendMessage("The response from the ChangeMyMind API was not properly formatted.").queue();
				}
			}

			@Override
			public void failed(UnirestException ue) {
				// Shouldn't happen
				ExceptionLogger.capture(ue, getClass().getSimpleName());
				event.getHook().sendMessage("The request to the ChangeMyMind API failed.").queue();
			}

			@Override
			public void cancelled() {
				// Shouldn't happen
				event.getHook().sendMessage("The request to the ChangeMyMind API was canceled.").queue();
			}
		});
	}
}