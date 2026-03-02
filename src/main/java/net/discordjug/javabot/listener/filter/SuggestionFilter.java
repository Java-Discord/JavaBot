package net.discordjug.javabot.listener.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.SystemsConfig;
import net.discordjug.javabot.util.MessageActionUtils;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * When a message is created in the {@link net.discordjug.javabot.data.config.guild.ModerationConfig#getSuggestionChannel()} channel, it is decorated as a suggestion.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SuggestionFilter implements MessageFilter {
	private final BotConfig botConfig;
	
	@Override
	public MessageModificationStatus processMessage(MessageContent content) {
		if (!canCreateSuggestion(content.event())) return MessageModificationStatus.NOT_MODIFIED;
		MessageEmbed embed = buildSuggestionEmbed(content.messageText().toString(), content.event().getMember());
		MessageActionUtils.addAttachmentsAndSend(content.attachments(), content.event().getChannel().sendMessageEmbeds(embed))
			.thenAccept(message -> {
					addReactions(message).queue();
					content.event().getMessage().delete().queue();
					message.createThreadChannel(String.format("%s — Suggestion", content.event().getAuthor().getName()))
							.flatMap(thread -> thread.addThreadMember(content.event().getAuthor()))
							.queue();
				}
		).exceptionally(e -> {
			log.error("Could not send Submission Embed", e);
			return null;
		});
		return MessageModificationStatus.STOP_PROCESSING;
	}
	
	/**
	 * Decides whether the message author is eligible to create new suggestions.
	 *
	 * @param event The {@link MessageReceivedEvent} that is fired upon sending a message.
	 * @return Whether the message author is eligible to create new suggestions.
	 */
	private boolean canCreateSuggestion(@NotNull MessageReceivedEvent event) {
		if (event.getChannelType() == ChannelType.PRIVATE) return false;
		return !event.getAuthor().isBot() && !event.getAuthor().isSystem() && !event.getMember().isTimedOut() &&
				event.getMessage().getType() != MessageType.THREAD_CREATED &&
				event.getChannel().getIdLong() == botConfig.get(event.getGuild()).getModerationConfig().getSuggestionChannelId();
	}

	/**
	 * Adds the upvote and Downvote emoji to the suggestion message.
	 *
	 * @param message The message that was sent.
	 * @return A {@link RestAction}.
	 */
	private RestAction<?> addReactions(Message message) {
		SystemsConfig.EmojiConfig config = botConfig.getSystems().getEmojiConfig();
		return RestAction.allOf(
				message.addReaction(config.getUpvoteEmote(message.getJDA())),
				message.addReaction(config.getDownvoteEmote(message.getJDA()))
		);
	}

	private MessageEmbed buildSuggestionEmbed(String messageContent, Member member) {
		// Note: member will never be null in practice. This is to satisfy code analysis tools.
		if (member == null) throw new IllegalStateException("Member was null when building suggestion embed.");
		return new EmbedBuilder()
				.setTitle("Suggestion")
				.setAuthor(member.getEffectiveName(), null, member.getEffectiveAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now())
				.setDescription(messageContent)
				.setFooter(member.getId())
				.build();
	}
	
	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}
}
