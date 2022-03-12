package net.javadiscord.javabot.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
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
		if (Bot.autoMod.hasSuspiciousLink(event.getMessage()) || Bot.autoMod.hasAdvertisingLink(event.getMessage())) {
			event.getMessage().delete().queue();
			return;
		}
		var config = Bot.config.get(event.getGuild());
		MessageEmbed embed = this.buildSuggestionEmbed(event.getMessage(), config.getSlashCommand());
		this.addAttachments(event.getMessage(), event.getChannel().sendMessageEmbeds(embed)).queue(message -> {
					this.addReactions(message).queue();
					event.getMessage().delete().queue();
					message.createThreadChannel(String.format("%s — Suggestion", event.getAuthor().getName())).queue();
				}, e -> log.error("Could not send Submission Embed", e)
		);
	}

	/**
	 * Decides whether the message author is eligible to create new suggestions.
	 *
	 * @param event The {@link MessageReceivedEvent} that is fired upon sending a message.
	 * @return Whether the message author is eligible to create new suggestions.
	 */
	private boolean canCreateSuggestion(MessageReceivedEvent event) {
		if (event.getChannelType() == ChannelType.PRIVATE) return false;
		return !event.getAuthor().isBot() && !event.getAuthor().isSystem() && !event.getMember().isTimedOut() &&
				event.getMessage().getType() != MessageType.THREAD_CREATED &&
				event.getChannel().equals(Bot.config.get(event.getGuild()).getModeration().getSuggestionChannel());
	}

	private MessageEmbed buildSuggestionEmbed(Message message, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setTitle("Suggestion")
				.setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
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

	private MessageAction addAttachments(Message message, MessageAction action) {
		for (Message.Attachment attachment : message.getAttachments()) {
			try {
				action.addFile(attachment.retrieveInputStream().get(), attachment.getFileName());
			} catch (InterruptedException | ExecutionException e) {
				action.append("Could not add Attachment: " + attachment.getFileName());
			}
		}
		return action;
	}
}
