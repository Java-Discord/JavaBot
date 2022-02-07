package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Listens for {@link MessageReceivedEvent}s in the
 * {@link net.javadiscord.javabot.data.config.guild.ModerationConfig#getSuggestionChannel()} channel.
 */
@Slf4j
public class SuggestionListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!canCreateSuggestion(event)) return;
		if (Bot.autoMod.hasSuspiciousLink(event.getMessage()) || Bot.autoMod.hasAdvertisingLink(event.getMessage())){
			event.getMessage().delete().queue();
			return;
		}
		var config = Bot.config.get(event.getGuild());
		var embed = buildSuggestionEmbed(event.getAuthor(), event.getMessage(), config.getSlashCommand());
		MessageAction action = event.getChannel().sendMessageEmbeds(embed);
		for (var a : event.getMessage().getAttachments()) {
			try {
				action.addFile(a.retrieveInputStream().get(), a.getFileName());
			} catch (InterruptedException | ExecutionException e) {
				action.append("Could not add Attachment: " + a.getFileName());
			}
		}
		action.queue(success -> {
					addReactions(success).queue();
					event.getMessage().delete().queue();
				}, e -> log.error("Could not send Submission Embed", e)
		);
	}

	private boolean canCreateSuggestion(MessageReceivedEvent event) {
		if (event.getChannelType() == ChannelType.PRIVATE) return false;
		return !event.getAuthor().isBot() && !event.getAuthor().isSystem() && event.getMessage().getType() != MessageType.THREAD_CREATED
				&& event.getChannel().equals(Bot.config.get(event.getGuild()).getModeration().getSuggestionChannel());
	}

	private MessageEmbed buildSuggestionEmbed(User user, Message message, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setTitle("Suggestion")
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setImage(null)
				.setColor(config.getDefaultColor())
				.setTimestamp(Instant.now())
				.setDescription(message.getContentRaw())
				.build();
	}

	private RestAction<?> addReactions(Message m) {
		var config = Bot.config.get(m.getGuild()).getEmote();
		return RestAction.allOf(
				m.addReaction(config.getUpvoteEmote()),
				m.addReaction(config.getDownvoteEmote())
		);
	}
}
