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
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

/**
 * Command that generates the "Change my mind" meme with the given text input.
 */
@Deprecated
public class ChangeMyMindCommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var hook = event.getHook();
		String encodedSearchTerm = null;
		try {
			encodedSearchTerm = URLEncoder.encode(Objects.requireNonNull(event.getOption("text")).getAsString(), StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
				}
			}

			@Override
			public void failed(UnirestException ue) {
				// Shouldn't happen
			}

			@Override
			public void cancelled() {
				// Shouldn't happen
			}
		});
		return event.deferReply(false);
	}
}