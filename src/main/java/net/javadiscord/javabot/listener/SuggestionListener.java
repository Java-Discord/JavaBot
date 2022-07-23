package net.javadiscord.javabot.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

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
		MessageEmbed embed = buildSuggestionEmbed(event.getMessage());
		MessageActionUtils.addAttachmentsAndSend(event.getMessage(), event.getChannel().sendMessageEmbeds(embed)).thenAccept(message -> {
					addReactions(message).queue();
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
				event.getChannel().equals(Bot.config.get(event.getGuild()).getModerationConfig().getSuggestionChannel());
	}

	/**
	 * Adds the upvote and Downvote emoji to the suggestion message.
	 *
	 * @param message The message that was sent.
	 * @return A {@link RestAction}.
	 */
	private RestAction<?> addReactions(Message message) {
		SystemsConfig.EmojiConfig config = Bot.config.getSystems().getEmojiConfig();
		return RestAction.allOf(
				message.addReaction(config.getUpvoteEmote(message.getJDA())),
				message.addReaction(config.getDownvoteEmote(message.getJDA()))
		);
	}



	private MessageEmbed buildSuggestionEmbed(Message message) {
		Member member = message.getMember();
		// Note: member will never be null in practice. This is to satisfy code analysis tools.
		if (member == null) throw new IllegalStateException("Member was null when building suggestion embed.");
		return new EmbedBuilder()
				.setTitle("Suggestion")
				.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now())
				.setDescription(message.getContentRaw())
				.build();
	}
}
