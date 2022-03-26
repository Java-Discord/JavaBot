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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
		this.addAttachmentsAndSend(event.getMessage(), event.getChannel().sendMessageEmbeds(embed)).thenAccept(message -> {
					this.addReactions(message).queue();
					event.getMessage().delete().queue();
					message.createThreadChannel(String.format("%s — Suggestion", event.getAuthor().getName()))
							.flatMap(thread -> thread.addThreadMember(event.getAuthor()))
							.queue();
				}
		).exceptionally(e -> {
			log.error("Could not send Submission Embed", e);
			return null;
		});
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

	/**
	 * Adds the upvote and Downvote emoji to the suggestion message.
	 *
	 * @param message The message that was sent.
	 * @return A {@link RestAction}.
	 */
	private RestAction<?> addReactions(Message message) {
		var config = Bot.config.get(message.getGuild()).getEmote();
		return RestAction.allOf(
				message.addReaction(config.getUpvoteEmote()),
				message.addReaction(config.getDownvoteEmote())
		);
	}

	/**
	 * Adds all Attachments from the initial message to the new message action and sends the message.
	 *
	 * @param message The initial {@link Message} object.
	 * @param action The new {@link MessageAction}.
	 * @return A {@link CompletableFuture} with the message that is being sent.
	 */
	private CompletableFuture<Message> addAttachmentsAndSend(Message message, MessageAction action) {
		List<CompletableFuture<?>> attachmentFutures = new ArrayList<>();
		for (Message.Attachment attachment : message.getAttachments()) {
			attachmentFutures.add(
					attachment.retrieveInputStream()
							.thenApply(is -> action.addFile(is, attachment.getFileName()))
							.exceptionally(e -> action.append("Could not add Attachment: " + attachment.getFileName()))
			);
		}
		return CompletableFuture.allOf(attachmentFutures.toArray(new CompletableFuture<?>[0]))
				.thenCompose(unusedActions -> action.submit());
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
}
