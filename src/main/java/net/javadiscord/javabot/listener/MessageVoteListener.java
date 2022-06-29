package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Generic listener that can be extended to add the ability for users to vote
 * on whether a message should stay in the channel.
 */
public abstract class MessageVoteListener extends ListenerAdapter {
	/**
	 * Gets the text channel in which this vote listener operates.
	 *
	 * @param guild The guild to get the channel for.
	 * @return The text channel that this vote listener should listen in.
	 */
	protected abstract TextChannel getChannel(Guild guild);

	/**
	 * Gets the threshold needed to remove a message. If a message has <code>U</code>
	 * upvotes and <code>D</code> downvotes, we compute the difference as
	 * <code>D - U</code> to get a number that indicates how many more
	 * downvotes there are than upvotes. If this value is higher than or equal
	 * to the threshold value returned by this method, the message is deleted.
	 * <p>
	 *     Note that usually, you want to return a positive value, to indicate
	 *     that the message should have <em>more</em> downvotes than upvotes.
	 * </p>
	 *
	 * @param guild The guild to get the threshold for.
	 * @return The delete threshold value.
	 */
	protected int getMessageDeleteVoteThreshold(Guild guild) {
		return 5;
	}

	/**
	 * Determines if a given message is eligible for voting. Only eligible
	 * messages will have voting reactions applied.
	 *
	 * @param message The message to check.
	 * @return True if the message is eligible for voting, or false if not.
	 */
	protected boolean isMessageEligibleForVoting(Message message) {
		return true;
	}

	/**
	 * Gets the emote that's used for casting upvotes.
	 *
	 * @param jda The {@link JDA} instance to get the emoji for.
	 * @return The emote.
	 */
	protected Emoji getUpvoteEmote(JDA jda) {
		return Bot.config.getSystems().getEmojiConfig().getUpvoteEmote(jda);
	}

	/**
	 * Gets the emote that's used for casting downvotes.
	 *
	 * @param jda The {@link JDA} instance to get the emoji for.
	 * @return The emote.
	 */
	protected Emoji getDownvoteEmote(JDA jda) {
		return Bot.config.getSystems().getEmojiConfig().getDownvoteEmote(jda);
	}

	/**
	 * Whether the bot should add the first upvote and downvote emotes to
	 * messages that are eligible for voting.
	 *
	 * @param guild The guild to get this setting for.
	 * @return True if the bot should add emotes.
	 */
	protected boolean shouldAddInitialEmotes(Guild guild) {
		return true;
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (isMessageReceivedEventValid(event) && shouldAddInitialEmotes(event.getGuild())) {
			event.getMessage().addReaction(getUpvoteEmote(event.getJDA())).queue();
			event.getMessage().addReaction(getDownvoteEmote(event.getJDA())).queue();
		}
	}

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		Bot.asyncPool.submit(() -> handleReactionEvent(event));
	}

	@Override
	public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
		Bot.asyncPool.submit(() -> handleReactionEvent(event));
	}

	/**
	 * Checks if a message received event is valid for this vote listener.
	 *
	 * @param event The event to check.
	 * @return True if the event is valid, meaning that it is relevant for this
	 * vote listener to add the voting emotes to it.
	 */
	private boolean isMessageReceivedEventValid(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem() || event.getMessage().getType() == MessageType.THREAD_CREATED) {
			return false;
		}
		return event.getChannel().getId().equals(getChannel(event.getGuild()).getId()) &&
				isMessageEligibleForVoting(event.getMessage());
	}

	/**
	 * Checks if a reaction event is valid for this vote listener. Note that
	 * this method may use blocking calls to check if the user who sent the
	 * reaction is valid.
	 *
	 * @param event The event to check.
	 * @return True if the event is valid, meaning that this listener should
	 * proceed to check the votes on the message.
	 */
	private boolean isReactionEventValid(@NotNull GenericMessageReactionEvent event) {
		if (!event.getChannel().getId().equals(getChannel(event.getGuild()).getId())) return false;
		Emoji reaction = event.getEmoji();
		if (
				!reaction.equals(getUpvoteEmote(event.getJDA())) &&
				!reaction.equals(getDownvoteEmote(event.getJDA()))
		) {
			return false;
		}

		User user = event.retrieveUser().complete();
		return !user.isBot() && !user.isSystem();
	}

	/**
	 * Handles voting reaction events, including both the addition and removal
	 * of votes. Note that this is a blocking method.
	 *
	 * @param event The reaction event to handle.
	 */
	private void handleReactionEvent(GenericMessageReactionEvent event) {
		if (isReactionEventValid(event)) {
			Message message = event.retrieveMessage().complete();
			if (isMessageEligibleForVoting(message)) {
				checkVotes(message, event.getGuild());
			}
		}
	}

	private void checkVotes(Message msg, @NotNull Guild guild) {
		Emoji upvoteId = getUpvoteEmote(guild.getJDA());
		Emoji downvoteId = getDownvoteEmote(guild.getJDA());

		int upvotes = StringUtils.countReactions(msg, upvoteId);
		int downvotes = StringUtils.countReactions(msg, downvoteId);
		int downvoteDifference = downvotes - upvotes;

		if (downvoteDifference >= getMessageDeleteVoteThreshold(guild)) {
			msg.delete().queue();
			msg.getAuthor().openPrivateChannel()
					.queue(
							s -> s.sendMessageFormat(
									"Your message in %s has been removed due to community feedback.",
									getChannel(guild).getAsMention()
							).queue(),
							e -> {}
					);
		}
	}
}
