package net.javadiscord.javabot.systems.help;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.config.guild.HelpForumConfig;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringResourceCache;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	/**
	 * The Constructor of the class.
	 *
	 * @param jda            The {@link JDA} instance.
	 * @param config         The bot's {@link HelpConfig}.
	 * @param semanticChecks A list with all {@link ChannelSemanticCheck}s.
	 */
	public HelpChannelUpdater(JDA jda, HelpConfig config, List<ChannelSemanticCheck> semanticChecks) {
		this.jda = jda;
		this.config = config;
		this.semanticChecks = semanticChecks;
		this.channelManager = new HelpChannelManager(config);
	}

	@Override
	public void run() {
		for (TextChannel channel : config.getReservedChannelCategory().getTextChannels()) {
			checkReservedChannel(channel).queue();
		}
		for (TextChannel channel : config.getOpenChannelCategory().getTextChannels()) {
			checkOpenChannel(channel).queue();
		}
		balanceChannels();
		// periodically update the Help Channel Overview Message
		if (config.getHelpOverviewMessageIds() != null && !config.getHelpOverviewMessageIds().isEmpty()) {
			updateHelpOverview();
		}
	}

	/**
	 * Performs a periodic check on a reserved help channel to see if we need to
	 * take certain actions.
	 *
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	@SuppressWarnings("unchecked")
	private RestAction<?> checkReservedChannel(@NotNull TextChannel channel) {
		Optional<ChannelReservation> optionalReservation = channelManager.getReservationForChannel(channel.getIdLong());
		if (optionalReservation.isEmpty()) {
			log.info("Unreserving channel {} because no reservation information about it could be found.", channel.getName());
			return channelManager.unreserveChannel(channel);
		} else {
			ChannelReservation reservation = optionalReservation.get();
			return channel.getJDA().retrieveUserById(reservation.getUserId()).flatMap(owner -> {
				if (owner == null) {
					log.info("Unreserving channel {} because no owner could be found.", channel.getName());
					return (RestAction<Object>) this.channelManager.unreserveChannel(channel);
				}
				return (RestAction<Object>) checkReservedChannelHistory(channel, owner, reservation);
			});
		}
	}

	/**
	 * Checks the recent chat history of a reserved help channel to determine
	 * if we need to take certain actions, like sending an activity check if the
	 * channel is inactive, or unreserving the channel if a pending activity
	 * check has not been responded to in a while.
	 *
	 * @param channel     The channel to check.
	 * @param owner       The owner of the channel.
	 * @param reservation The channel reservation data.
	 * @return A rest action that completes when this check is done.
	 */
	private RestAction<?> checkReservedChannelHistory(TextChannel channel, User owner, ChannelReservation reservation) {
		return channel.getHistory().retrievePast(50).map(messages -> {
			Message mostRecentMessage = messages.isEmpty() ? null : messages.get(0);
			if (mostRecentMessage == null) {
				log.info("Unreserving channel {} because no recent messages could be found.", channel.getName());
				return this.channelManager.unreserveChannel(channel);
			}
			try {
				// Check if the most recent message is a channel inactivity check, and check that it's old enough to surpass the remove timeout.
				if (isActivityCheck(mostRecentMessage)) {
					if (mostRecentMessage.getTimeCreated().plusMinutes(config.getRemoveInactiveTimeoutMinutes()).isBefore(OffsetDateTime.now())) {
						log.info("Unreserving channel {} because of no response to activity check.", channel.getName());
						return unreserveInactiveChannel(channel, owner, mostRecentMessage, messages);
					}
				} else if (isThankMessage(mostRecentMessage)) {
					if (mostRecentMessage.getTimeCreated().plusMinutes(config.getRemoveThanksTimeoutMinutes()).isBefore(OffsetDateTime.now())) {
						log.info("Unreserving channel {} because no response to thanks question was received.", channel.getName());
						return unreserveInactiveChannel(channel, owner, mostRecentMessage, messages);
					}
				} else {// The most recent message is not an activity check, so check if it's old enough to warrant sending an activity check.
					int timeout = channelManager.getTimeout(channel);
					if (mostRecentMessage.getTimeCreated().plusMinutes(timeout).isBefore(OffsetDateTime.now())) {
						if (isActivityCheckAffirmativeResponse(mostRecentMessage)) { //If the last message was an activity check affirmative response, delete that and send a new check
							mostRecentMessage.delete().queue();
						}
						return sendActivityCheck(channel, owner, reservation);
					} else {// The channel is still active, so take this opportunity to clean up the channel.
						// Also use it to do some introspection on the type of messages sent recently, to see if the bot can provide automated guidance.
						return RestAction.allOf(deleteOldBotMessages(messages), semanticMessageCheck(channel, owner, messages));
					}
				}
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				return new CompletedRestAction<>(this.jda, e);
			}
			// No action needed.
			return new CompletedRestAction<>(this.jda, null);
		}).flatMap(action -> action);
	}

	/**
	 * Checks an open help channel to ensure it's in the correct state.
	 *
	 * @param channel The channel to check.
	 * @return A rest action that completes when the check is done.
	 */
	private @NotNull RestAction<?> checkOpenChannel(TextChannel channel) {
		if (!this.config.isRecycleChannels()) {
			return channel.getHistory().retrievePast(1).map(messages -> {
				Message lastMessage = messages.isEmpty() ? null : messages.get(0);
				if (lastMessage != null) {
					// If we're not recycling channels, we want to keep all open channels fresh.
					// Any open channel with a message in it should be immediately become reserved.
					// However, network issues or other things could cause this to fail, so we clean up here.
					log.info("Removing non-empty open channel {}.", channel.getName());
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
				List<TextChannel> dormantChannels = this.config.getDormantChannelCategory().getTextChannels();
				if (!dormantChannels.isEmpty()) {
					Category target = this.config.getOpenChannelCategory();
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
			TextChannel channel = this.config.getOpenChannelCategory().getTextChannels().get(0);
			if (this.config.isRecycleChannels()) {
				Category target = this.config.getDormantChannelCategory();
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
	 *
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
	 *
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
	 *
	 * @param message The message to check.
	 * @return True if the message is a reservation message.
	 */
	private boolean isReservationMessage(Message message) {
		return message.getAuthor().equals(this.jda.getSelfUser()) &&
				config.getReservedChannelMessage() != null &&
				message.getContentRaw().contains(config.getReservedChannelMessage());
	}

	/**
	 * Determines if a message is a "thank" message that's sent when a user
	 * unreserves their channel.
	 *
	 * @param message The message to check.
	 * @return True if the message is a thank message.
	 */
	private boolean isThankMessage(Message message) {
		return message.getAuthor().equals(this.jda.getSelfUser()) &&
				message.getContentRaw().equals(HelpChannelManager.THANK_MESSAGE_TEXT);
	}

	/**
	 * Sends an activity check to the given channel, to check that the owner is
	 * still using the channel.
	 *
	 * @param channel     The channel to send the check to.
	 * @param owner       The owner of the channel.
	 * @param reservation The channel reservation data.
	 * @return A rest action that completes when the check has been sent.
	 */
	private RestAction<?> sendActivityCheck(TextChannel channel, User owner, ChannelReservation reservation) {
		log.info("Sending inactivity check to {} because of no activity since timeout.", channel.getName());
		return channel.sendMessage(String.format(ACTIVITY_CHECK_MESSAGE, owner.getAsMention(), config.getRemoveInactiveTimeoutMinutes()))
				.setActionRow(
						new ButtonImpl("help-channel:" + reservation.getId() + ":done", "Yes, I'm done here!", ButtonStyle.SUCCESS, false, Emoji.fromUnicode("\u2705")),
						new ButtonImpl("help-channel:" + reservation.getId() + ":not-done", "No, I'm still using it.", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("\u274C"))
				);
	}

	/**
	 * Unreserves an inactive channel, which happens after a user ignores the
	 * activity check for some amount of time.
	 *
	 * @param channel           The channel to unreserve.
	 * @param owner             The owner of the channel.
	 * @param mostRecentMessage The most recent message that was sent.
	 * @param messages          The list of all recent messages, so that the bot can do
	 *                          some cleanup if needed.
	 * @return A rest action that completes once the channel is unreserved.
	 */
	private RestAction<?> unreserveInactiveChannel(TextChannel channel, User owner, Message mostRecentMessage, List<Message> messages) {
		log.info("Unreserving channel {} because of inactivity for {} minutes following inactive check.", channel.getName(), config.getRemoveInactiveTimeoutMinutes());
		return RestAction.allOf(
				mostRecentMessage.delete(),
				deleteThankMessages(messages),
				channel.sendMessage(String.format(
						"%s, this channel will be unreserved due to prolonged inactivity. If your question still isn't answered, please ask again in an open channel.",
						owner.getAsMention()
				)),
				this.channelManager.unreserveChannel(channel)
		);
	}

	/**
	 * Deletes any "thank-you" message that the bot sent previously.
	 *
	 * @param messages The list of messages to search through.
	 * @return A rest action that completes once all thank messages are deleted.
	 */
	private @NotNull RestAction<?> deleteThankMessages(@NotNull List<Message> messages) {
		List<AuditableRestAction<Void>> deleteActions = messages.stream()
				.filter(this::isThankMessage)
				.map(Message::delete).toList();
		if (!deleteActions.isEmpty()) return RestAction.allOf(deleteActions);
		return new CompletedRestAction<>(this.jda, null);
	}

	/**
	 * Removes all old activity check messages from the list of messages.
	 *
	 * @param messages The messages to remove activity checks from.
	 * @return A rest action that completes when all activity checks are removed.
	 */
	private RestAction<?> deleteOldBotMessages(List<Message> messages) {
		List<AuditableRestAction<Void>> deleteActions = messages.stream()
				.filter(m -> isActivityCheck(m))
				.map(Message::delete).toList();
		if (!deleteActions.isEmpty()) {
			return RestAction.allOf(deleteActions);
		}
		return new CompletedRestAction<>(this.jda, null);
	}

	/**
	 * Performs checks on the recent message history of a channel.
	 *
	 * @param channel  The channel that the messages belong to.
	 * @param owner    The user who's reserved the channel.
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
			final Message fm = firstMessage;
			messages.removeIf(m -> m.getTimeCreated().isBefore(fm.getTimeCreated()));
			botMessages.removeIf(m -> m.getTimeCreated().isBefore(fm.getTimeCreated()));
		}
		// Trim away system messages from the main list of messages.
		messages.removeIf(m -> m.getAuthor().isSystem());
		List<User> nonOwnerParticipants = messages.stream().map(Message::getAuthor).filter(u -> !u.isBot() && !u.isSystem()).toList();
		Duration timeSinceFirstMessage = null;
		if (firstMessage != null) {
			timeSinceFirstMessage = Duration.between(firstMessage.getTimeCreated(), OffsetDateTime.now());
		}
		ChannelSemanticData data = new ChannelSemanticData(firstMessage, timeSinceFirstMessage, nonOwnerParticipants, botMessages);
		List<RestAction<?>> checkActions = semanticChecks.stream()
				.map(c -> c.doCheck(channel, owner, messages, data))
				.collect(Collectors.toList());
		if (checkActions.isEmpty()) return new CompletedRestAction<>(jda, null);
		return RestAction.allOf(checkActions);
	}

	private void updateHelpOverview() {
		HelpForumConfig forumConfig = Bot.getConfig().get(config.getGuild()).getHelpForumConfig();
		config.getHelpOverviewMessageIds().forEach((channelId, messageId) -> {
			TextChannel channel = config.getGuild().getTextChannelById(channelId);
			if (channel == null) {
				log.error("Could not find Help Overview Channel with id '{}'", channelId);
				return;
			}
			List<TextChannel> availableChannels = config.getOpenChannelCategory().getTextChannels();
			List<Button> buttons = new ArrayList<>(2);
			if (!availableChannels.isEmpty()) {
				buttons.add(Button.link(availableChannels.get(0).getJumpUrl(), "Show me an Available Help Channel!"));
			}
			buttons.add(Button.link(StringResourceCache.load("/help_overview/overview_image_url.txt"), "How does this work?"));
			channel.retrieveMessageById(messageId).queue(
					m -> m.editMessageEmbeds(buildHelpOverviewEmbed()).setComponents(
							ActionRow.of(
									// Temporary Forum Channel Upsell
									Button.link(forumConfig.getHelpForumChannel().getJumpUrl(), "Try our new Help Forum!"),
									Button.link("https://discord.com/blog/forum-channels-space-for-organized-conversation", "What are Forums?")
							),
							ActionRow.of(buttons)).queue(),
					err -> channel.sendMessageEmbeds(buildHelpOverviewEmbed()).queue(m -> {
						config.getHelpOverviewMessageIds().put(channelId, m.getIdLong());
						Bot.getConfig().flush();
						log.info("Successfully created new Help Overview Message in '{}' on message with id '{}'", channelId, m.getId());
						ExceptionLogger.capture(err);
						Bot.getConfig().get(m.getGuild()).getModerationConfig().getLogChannel().sendMessage("Sent new help-overview message (Check sentry for cause!)").queue();
					})
			);
		});
	}

	private @NotNull MessageEmbed buildHelpOverviewEmbed() {
		String availableHelpChannels = config.getOpenChannelCategory().getTextChannels()
				.stream()
				.map(TextChannel::getAsMention)
				.collect(Collectors.joining(", "));
		StringBuilder reservedHelpChannels = new StringBuilder();
		for (TextChannel channel : config.getReservedChannelCategory().getTextChannels()) {
			Optional<ChannelReservation> optional = channelManager.getReservationForChannel(channel.getIdLong());
			if (optional.isEmpty()) continue;
			ChannelReservation reservation = optional.get();
			// Attempt to fetch the user. As JDA will cache the user upon retrieval, this may be too 'slow' on the first run (for the appending),
			// but will automatically fix itself after the embed is updated again.
			jda.retrieveUserById(reservation.getUserId()).queue(
					user -> reservedHelpChannels.append(String.format("""
									%s
									Reserved by %s <t:%s:R>
																
									""", channel.getAsMention(), user.getAsMention(),
							reservation.getReservedAt().toEpochSecond(ZoneOffset.UTC))),
					e -> ExceptionLogger.capture(e, getClass().getSimpleName())
			);
		}
		ForumChannel forum = Bot.getConfig().get(config.getGuild()).getHelpForumConfig().getHelpForumChannel();
		EmbedBuilder builder = new EmbedBuilder()
				.setTitle("Help Overview")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(availableHelpChannels.isEmpty() ?
						String.format("There are no help channels available to claim.%nHow about using our new **[Help Forum](%s)** (%s) then?",
								forum.getJumpUrl(), forum.getAsMention()) :
						availableHelpChannels + " are __**available**__ to claim!")
				.setFooter("Last refreshed: ")
				.setTimestamp(Instant.now());
		if (!reservedHelpChannels.isEmpty()) {
			builder.addField("Reserved Help Channels", reservedHelpChannels.toString(), false);
		}
		if (!config.getDormantChannelCategory().getTextChannels().isEmpty()) {
			builder.addField("Dormant Help Channels", String.format("%s dormant channels", config.getDormantChannelCategory().getTextChannels().size()), false);
		}
		return builder.build();
	}
}
