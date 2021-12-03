package com.javadiscord.javabot.service.help;

import com.javadiscord.javabot.data.properties.config.guild.HelpConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.interactions.ButtonImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;

import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task that updates all help channels in a particular guild.
 */
@Slf4j
public class HelpChannelUpdater implements Runnable {
	private static final String ACTIVITY_CHECK_MESSAGE = "Hey %s, it looks like this channel is inactive. Are you finished with this channel?\n\n> _If no response is received after %d minutes, this channel will be removed._";

	private final JDA jda;
	private final HelpConfig config;
	private final HelpChannelManager channelManager;
	private final List<ChannelSemanticCheck> semanticChecks;

	public HelpChannelUpdater(JDA jda, HelpConfig config, List<ChannelSemanticCheck> semanticChecks) {
		this.jda = jda;
		this.config = config;
		this.semanticChecks = semanticChecks;
		this.channelManager = new HelpChannelManager(config);
	}

	@Override
	public void run() {
		for (var channel : config.getReservedChannelCategory().getTextChannels()) {
			this.checkReservedChannel(channel).queue();
		}
		for (var channel : config.getOpenChannelCategory().getTextChannels()) {
			this.checkOpenChannel(channel).queue();
		}
		this.balanceChannels();
	}

	/**
	 * Performs checks on a reserved channel. This will do several things:
	 * <ul>
	 *     <li>
	 *         If the most recent message in the channel is old enough, it will
	 *         send an activity check message in the channel, asking the user to
	 *         confirm whether they're still using it.
	 *     </li>
	 *     <li>
	 *         If the most recent message is an activity check message, and it
	 *         has stuck around long enough without any response, then the
	 *         channel will be unreserved.
	 *     </li>
	 *     <li>
	 *         If for some reason we can't retrieve the owner of the channel,
	 *         like if they left the server, the channel will be unreserved.
	 *     </li>
	 * </ul>
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	private RestAction<?> checkReservedChannel(TextChannel channel) {
		User owner = this.channelManager.getReservedChannelOwner(channel);
		if (owner == null) {
			log.info("Unreserving channel {} because no owner could be found.", channel.getAsMention());
			return this.channelManager.unreserveChannel(channel);
		}
		return channel.getHistory().retrievePast(50).map(messages -> {
			Message mostRecentMessage = messages.isEmpty() ? null : messages.get(0);
			if (mostRecentMessage == null) {
				log.info("Unreserving channel {} because no recent messages could be found.", channel.getAsMention());
				return this.channelManager.unreserveChannel(channel);
			}

			try {
				// Check if the most recent message is a channel inactivity check, and check that it's old enough to surpass the remove timeout.
				if (isActivityCheck(mostRecentMessage)) {
					if (mostRecentMessage.getTimeCreated().plusMinutes(config.getRemoveTimeoutMinutes()).isBefore(OffsetDateTime.now())) {
						return unreserveInactiveChannel(channel, owner, mostRecentMessage);
					}
				} else {// The most recent message is not an activity check, so check if it's old enough to warrant sending an activity check.
					int timeout = channelManager.getTimeout(channel);
					if (mostRecentMessage.getTimeCreated().plusMinutes(timeout).isBefore(OffsetDateTime.now())) {
						return sendActivityCheck(channel, owner);
					} else {// The channel is still active, so take this opportunity to clean up the channel.
						// Also use it to do some introspection on the type of messages sent recently, to see if the bot can provide automated guidance.
						return RestAction.allOf(deleteOldBotMessages(messages), semanticMessageCheck(channel, owner, messages));
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			// No action needed.
			return new CompletedRestAction<>(this.jda, null);
		}).flatMap(action->action);
	}

	/**
	 * Checks an open help channel to ensure it's in the correct state.
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	private RestAction<?> checkOpenChannel(TextChannel channel) {
		if (!this.config.isRecycleChannels()) {
			return channel.getHistory().retrievePast(1).map(messages -> {
				var lastMessage = messages.isEmpty() ? null : messages.get(0);
				if (lastMessage != null) {
					// If we're not recycling channels, we want to keep all open channels fresh.
					// Any open channel with a message in it should be immediately become reserved.
					// However, network issues or other things could cause this to fail, so we clean up here.
					log.info("Removing non-empty open channel {}.", channel.getAsMention());
					return channel.delete();
				} else {
					return new CompletedRestAction<>(this.jda, null);
				}
			});
		} else {
			return new CompletedRestAction<>(this.jda, null);
		}
	}

	/**
	 * Tries to move channels around to attain the preferred open channel count.
	 */
	private void balanceChannels() {
		int openChannelCount = this.channelManager.getOpenChannelCount();
		while (openChannelCount < this.config.getPreferredOpenChannelCount()) {
			if (this.config.isRecycleChannels()) {
				var dormantChannels = this.config.getDormantChannelCategory().getTextChannels();
				if (!dormantChannels.isEmpty()) {
					var target = this.config.getOpenChannelCategory();
					dormantChannels.get(0).getManager().setParent(target).sync(target).queue();
					openChannelCount++;
				} else {
					log.warn("Could not find a dormant channel to replenish open channels.");
					break;
				}
			} else {
				channelManager.openNew();
				openChannelCount++;
			}
		}
		while (openChannelCount > this.config.getPreferredOpenChannelCount()) {
			var channel = this.config.getOpenChannelCategory().getTextChannels().get(0);
			if (this.config.isRecycleChannels()) {
				var target = this.config.getDormantChannelCategory();
				channel.getManager().setParent(target).sync(target).queue();
			} else {
				channel.delete().queue();
			}
			openChannelCount--;
		}
	}

	/**
	 * Determines if a message is an 'activity check', which is a special type
	 * of message the bot sends to users to check if they're still using a help
	 * channel.
	 * @param message The message to check.
	 * @return True if the message is an activity check or false otherwise.
	 */
	private boolean isActivityCheck(Message message) {
		return message.getAuthor().equals(this.jda.getSelfUser()) &&
			message.getContentRaw().contains("Are you finished with this channel?");
	}

	/**
	 * Determines if a message is an activity check affirmative response, which
	 * the bot usually sends when a user indicates they'd like to keep their
	 * channel reserved.
	 * @param message The message to check.
	 * @return True if the message is an affirmative response to an activity
	 * check interaction.
	 */
	private boolean isActivityCheckAffirmativeResponse(Message message) {
		return message.getAuthor().equals(this.jda.getSelfUser()) &&
			message.getContentRaw().contains("Okay, we'll keep this channel reserved for you");
	}

	/**
	 * Determines if a message is a channel reservation message that's sent when
	 * a user first reserves a channel.
	 * @param message The message to check.
	 * @return True if the message is a reservation message.
	 */
	private boolean isReservationMessage(Message message) {
		return message.getAuthor().equals(this.jda.getSelfUser()) &&
				config.getReservedChannelMessage() != null &&
				message.getContentRaw().contains(config.getReservedChannelMessage());
	}

	/**
	 * Sends an activity check to the given channel, to check that the owner is
	 * still using the channel.
	 * @param channel The channel to send the check to.
	 * @param owner The owner of the channel.
	 * @return A rest action that completes when the check has been sent.
	 */
	private RestAction<?> sendActivityCheck(TextChannel channel, User owner) {
		log.info("Sending inactivity check to {} because of no activity since timeout.", channel.getAsMention());
		return channel.sendMessage(String.format(ACTIVITY_CHECK_MESSAGE, owner.getAsMention(), config.getRemoveTimeoutMinutes()))
			.setActionRow(
				new ButtonImpl("help-channel:done", "Yes, I'm done here!", ButtonStyle.SUCCESS, false, null),
				new ButtonImpl("help-channel:not-done", "No, I'm still using it.", ButtonStyle.DANGER, false, null)
			);
	}

	/**
	 * Unreserves an inactive channel, which happens after a user ignores the
	 * activity check for some amount of time.
	 * @param channel The channel to unreserve.
	 * @param owner The owner of the channel.
	 * @param mostRecentMessage The most recent message that was sent.
	 * @return A rest action that completes once the channel is unreserved.
	 */
	private RestAction<?> unreserveInactiveChannel(TextChannel channel, User owner, Message mostRecentMessage) {
		log.info("Unreserving channel {} because of inactivity for {} minutes following inactive check.", channel.getAsMention(), config.getRemoveTimeoutMinutes());
		return RestAction.allOf(
			mostRecentMessage.delete(),
			channel.sendMessage(String.format(
				"%s, this channel will be unreserved due to prolonged inactivity. If your question still isn't answered, please ask again in an open channel.",
				owner.getAsMention()
			)),
			this.channelManager.unreserveChannel(channel)
		);
	}

	/**
	 * Removes all old activity check messages from the list of messages.
	 * @param messages The messages to remove activity checks from.
	 * @return A rest action that completes when all activity checks are removed.
	 */
	private RestAction<?> deleteOldBotMessages(List<Message> messages) {
		var deleteActions = messages.stream()
			.filter(m -> isActivityCheck(m) || isActivityCheckAffirmativeResponse(m))
			.map(Message::delete).toList();
		if (!deleteActions.isEmpty()) {
			return RestAction.allOf(deleteActions);
		}
		return new CompletedRestAction<>(this.jda, null);
	}

	/**
	 * Performs checks on the recent message history of a channel.
	 * @param channel The channel that the messages belong to.
	 * @param owner The user who's reserved the channel.
	 * @param messages The list of messages to analyze, ordered from newest to
	 *                 oldest.
	 * @return A rest action that completes when this check is done.
	 */
	private RestAction<?> semanticMessageCheck(TextChannel channel, User owner, List<Message> messages) {
		Message firstMessage = null;
		for (int i = 0; i < messages.size(); i++) {
			if (i < messages.size() - 1 && isReservationMessage(messages.get(i))) {
				firstMessage = messages.get(i + 1);
				break;
			}
		}
		List<Message> botMessages = messages.stream()
				.filter(m -> m.getAuthor().equals(jda.getSelfUser()))
				.collect(Collectors.toCollection(ArrayList::new));
		// Trim away messages from before the owner's first message.
		if (firstMessage != null) {
			final var fm = firstMessage;
			messages.removeIf(m -> m.getTimeCreated().isBefore(fm.getTimeCreated()));
			botMessages.removeIf(m -> m.getTimeCreated().isBefore(fm.getTimeCreated()));
		}
		// Trim away bot messages from the main list of messages.
		messages.removeIf(m -> m.getAuthor().isBot() || m.getAuthor().isSystem());
		List<User> nonOwnerParticipants = messages.stream().map(Message::getAuthor).filter(u -> !u.isBot() && !u.isSystem()).toList();
		Duration timeSinceFirstMessage = null;
		if (firstMessage != null) {
			timeSinceFirstMessage = Duration.between(firstMessage.getTimeCreated(), OffsetDateTime.now());
		}
		var data = new ChannelSemanticData(firstMessage, timeSinceFirstMessage, nonOwnerParticipants, botMessages);
		List<RestAction<?>> checkActions = semanticChecks.stream()
				.map(c -> c.doCheck(channel, owner, messages, data))
				.collect(Collectors.toList());
		if (checkActions.isEmpty()) return new CompletedRestAction<>(jda, null);
		return RestAction.allOf(checkActions);
	}
}
