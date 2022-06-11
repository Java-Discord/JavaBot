package net.javadiscord.javabot.systems.commands;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import org.json.JSONException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * Command that generates the "Change my mind" meme with the given text input.
 */
@Deprecated
public class ChangeMyMindCommand implements SlashCommand {
	/**
	 * The maximum acceptable length for texts to send to the API. Technically,
	 * the API supports up to but not including 2000, but we'll make that much
	 * lower to avoid issues, since the API fails with a 501 error. People
	 * shouldn't really be putting paragraphs into this anyway.
	 */
	private static final int MAX_SEARCH_TERM_LENGTH = 1500;

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var hook = event.getHook();
		String encodedSearchTerm;
		encodedSearchTerm = URLEncoder.encode(Objects.requireNonNull(event.getOption("text")).getAsString(), StandardCharsets.UTF_8);
		if (encodedSearchTerm.toCharArray().length > MAX_SEARCH_TERM_LENGTH) {
			return event.reply("The text you provided is too long. It may not be more than " + MAX_SEARCH_TERM_LENGTH + " characters.");
		}

		Unirest.get("https://nekobot.xyz/api/imagegen?type=changemymind&text=" + encodedSearchTerm).asJsonAsync(new Callback<>() {
			@Override
			public void completed(HttpResponse<JsonNode> hr) {

				MessageEmbed e;
				try {
					e = new EmbedBuilder()
							.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
							.setImage(hr.getBody().getObject().getString("message"))
							.setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
							.setTimestamp(Instant.now())
							.build();
					hook.sendMessageEmbeds(e).queue();
				} catch (JSONException jsonException) {
					jsonException.printStackTrace();
					hook.sendMessage("The response from the ChangeMyMind API was not properly formatted.").queue();
				}
			}

			@Override
			public void failed(UnirestException ue) {
				// Shouldn't happen
				ue.printStackTrace();
				hook.sendMessage("The request to the ChangeMyMind API failed.").queue();
			}

			@Override
			public void cancelled() {
				// Shouldn't happen
			}
		});
		return event.deferReply(false);
	}
}