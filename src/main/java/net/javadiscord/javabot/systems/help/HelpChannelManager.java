package net.javadiscord.javabot.systems.help;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.MessageActionUtils;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * This manager is responsible for all the main interactions that affect the
 * help system's channels.
 */
@Slf4j
public class HelpChannelManager {
	/**
	 * Static String that contains the Thank Message Text.
	 */
	public static final String THANK_MESSAGE_TEXT = "Before your channel will be unreserved, would you like to express your gratitude to any of the people who helped you? When you're done, click **Unreserve**.";

	@Getter
	private final HelpConfig config;
	private final ScheduledExecutorService asyncPool;
	private final TextChannel logChannel;
	private final DataSource dataSource;
	private final DbActions dbActions;
	private final HelpExperienceService helpExperienceService;

	/**
	 * Initializes the {@link HelpChannelManager} object.
	 * @param botConfig The main configuration of the bot
	 * @param guild the {@link Guild} help channels should be managed in
	 * @param dbActions A service object responsible for various operations on the main database
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param helpExperienceService Service object that handles Help Experience Transactions.
	 */
	public HelpChannelManager(BotConfig botConfig, Guild guild, DbActions dbActions, ScheduledExecutorService asyncPool, HelpExperienceService helpExperienceService) {
		this.config = botConfig.get(guild).getHelpConfig();
		this.dataSource = dbActions.getDataSource();
		this.dbActions = dbActions;
		this.asyncPool = asyncPool;
		this.logChannel = botConfig.get(guild).getModerationConfig().getLogChannel();
		this.helpExperienceService = helpExperienceService;
	}

	public boolean isOpen(TextChannel channel) {
		return config.getOpenChannelCategory().equals(channel.getParentCategory());
	}

	public boolean isReserved(TextChannel channel) {
		return config.getReservedChannelCategory().equals(channel.getParentCategory());
	}

	public int getOpenChannelCount() {
		return config.getOpenChannelCategory().getTextChannels().size();
	}

	/**
	 * Determines if the given user is allowed to reserve a help channel.
	 *
	 * @param user The user who is trying to reserve a channel.
	 * @return True if the user can reserve it, or false if not.
	 */
	public boolean mayUserReserveChannel(User user) {
		Member member = this.config.getGuild().getMember(user);
		// Only allow guild members.
		if (member == null) return false;
		// Don't allow muted users.
		if (member.isTimedOut()) return false;
		try (Connection con = dataSource.getConnection(); PreparedStatement stmt = con.prepareStatement("SELECT COUNT(channel_id) FROM reserved_help_channels WHERE user_id = ?")) {
			stmt.setLong(1, user.getIdLong());
			ResultSet rs = stmt.executeQuery();
			return rs.next() && rs.getLong(1) < this.config.getMaxReservedChannelsPerUser();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			logChannel.sendMessage("Error while checking if a user can reserve a help channel: " + e.getMessage()).queue();
			return false;
		}
	}

	/**
	 * Opens a text channel so that it is ready for a new question.
	 */
	public void openNew() {
		Category category = config.getOpenChannelCategory();
		if (category == null) {
			throw new IllegalStateException("Missing help channel category. Cannot open a new help channel.");
		}
		String name = this.config.getChannelNamingStrategy().getName(category.getTextChannels(), config);
		category.createTextChannel(name).queue(channel -> {
			channel.getManager().setPosition(0).setTopic(this.config.getOpenChannelTopic()).queue();
			log.info("Created new help channel {}.", channel.getAsMention());
		});
	}

	/**
	 * Reserves a text channel for a user.
	 *
	 * @param channel       The channel to reserve.
	 * @param reservingUser The user who is reserving the channel.
	 * @param message       The message the user sent in the channel.
	 */
	public void reserve(TextChannel channel, User reservingUser, Message message) throws SQLException {
		if (!isOpen(channel)) throw new IllegalArgumentException("Can only reserve open channels!");
		// Check if the database still has this channel marked as reserved (which can happen if an admin manually moves a channel.)
		long alreadyReservedCount = dbActions.count(
				"SELECT COUNT(id) FROM reserved_help_channels WHERE channel_id = ?",
				s -> s.setLong(1, channel.getIdLong())
		);
		// If it's marked in the DB as reserved, remove that so that we can reserve it anew.
		if (alreadyReservedCount > 0) {
			dbActions.update("DELETE FROM reserved_help_channels WHERE channel_id = ?", channel.getIdLong());
		}
		int timeout = config.getInactivityTimeouts().get(0);
		dbActions.update(
				"INSERT INTO reserved_help_channels (channel_id, user_id, timeout) VALUES (?, ?, ?)",
				channel.getIdLong(), reservingUser.getIdLong(), timeout
		);
		Category target = config.getReservedChannelCategory();
		channel.getManager().setParent(target).sync(target).queue();
		// Pin the message, then immediately try and delete the annoying "message has been pinned" message.
		message.pin().queue(unused -> channel.getHistory().retrievePast(10).queue(messages -> {
			for (Message msg : messages) {
				if (msg.getType() == MessageType.CHANNEL_PINNED_ADD) {
					msg.delete().queue();
				}
			}
		}));
		if (config.getReservedChannelMessage() != null) {
			message.reply(config.getReservedChannelMessage()).queue();
		}
		log.info("Reserved channel {} for {}.", channel.getAsMention(), reservingUser.getAsTag());

		// Now that an open channel has been reserved, try and compensate by creating a new one or pulling one from storage.
		if (this.config.isRecycleChannels()) {
			List<TextChannel> dormantChannels = this.config.getDormantChannelCategory().getTextChannels();
			if (!dormantChannels.isEmpty()) {
				Category targetCategory = this.config.getOpenChannelCategory();
				TextChannel targetChannel = dormantChannels.get(0);
				targetChannel.getManager().setParent(targetCategory).sync(targetCategory).queue();
				targetChannel.sendMessage(config.getReopenedChannelMessage()).queue();
			}
		} else {
			this.openNew();
		}
	}

	/**
	 * Gets the owner of a reserved channel.
	 *
	 * @param channel The channel to get the owner of.
	 * @return The user who reserved the channel, or null.
	 */
	public User getReservedChannelOwner(TextChannel channel) {
		try {
			return dbActions.mapQuery(
					"SELECT user_id FROM reserved_help_channels WHERE channel_id = ?",
					s -> s.setLong(1, channel.getIdLong()),
					rs -> {
						if (rs.next()) return channel.getJDA().retrieveUserById(rs.getLong(1)).complete();
						return null;
					}
			);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return null;
		}
	}

	/**
	 * Gets a list of all users that have participated in a reserved help
	 * channel since it was reserved.
	 *
	 * @param channel The channel to get participants for.
	 * @return The list of users.
	 */
	public CompletableFuture<Map<Member, List<Message>>> getParticipantsSinceReserved(@NotNull TextChannel channel) {
		int limit = 300;
		MessageHistory history = channel.getHistory();
		final CompletableFuture<Map<Member, List<Message>>> cf = new CompletableFuture<>();
		asyncPool.execute(() -> {
			final Map<Member, List<Message>> userMessages = new HashMap<>();
			boolean endFound = false;
			while (!endFound && history.size() < limit) {
				List<Message> messages = history.retrievePast(50).complete();
				for (Message msg : messages) {
					if (msg.getContentRaw().contains(config.getReservedChannelMessage()) || msg.isPinned()) {
						endFound = true;
						break;
					}
					User user = msg.getAuthor();
					if (!user.isBot() && !user.isSystem()) {
						Member member = channel.getGuild().retrieveMember(user).complete();
						List<Message> um = userMessages.computeIfAbsent(member, u -> new ArrayList<>());
						um.add(msg);
					}
				}
			}
			cf.complete(userMessages);
		});
		return cf;
	}

	/**
	 * Unreserves a channel from the case that a user has done so via a discord
	 * interaction.
	 *
	 * @param channel     The channel to unreserve.
	 * @param owner       The owner of the reserved channel.
	 * @param reason      The user-supplied reason for unreserving the channel.
	 * @param interaction The interaction the user did to unreserve the channel.
	 */
	public void unreserveChannelByOwner(TextChannel channel, @NotNull User owner, @Nullable String reason, @NotNull Interaction interaction) {
		if (owner.equals(interaction.getUser())) {// The user is unreserving their own channel.
			unreserveChannelByOwner(channel, owner, interaction);
		} else {// The channel was unreserved by someone other than the owner.
			unreserveChannelByOtherUser(channel, owner, reason, (SlashCommandInteractionEvent) interaction);
		}
	}

	private void unreserveChannelByOwner(@NotNull TextChannel channel, User owner, Interaction interaction) {
		Optional<ChannelReservation> optionalReservation = getReservationForChannel(channel.getIdLong());
		if (optionalReservation.isEmpty()) {
			log.warn("Could not find current reservation data for channel {}. Unreserving the channel without thanks.", channel.getAsMention());
			unreserveChannel(channel);
			return;
		}
		ChannelReservation reservation = optionalReservation.get();
		// Ask the user for some feedback about the help channel, if possible.
		getParticipantsSinceReserved(channel).thenAcceptAsync(participants -> {
			List<Member> potentialHelpers = new ArrayList<>(participants.size());
			for (Map.Entry<Member, List<Message>> entry : participants.entrySet()) {
				if (!entry.getKey().getUser().equals(owner)) potentialHelpers.add(entry.getKey());
			}
			if (potentialHelpers.isEmpty()) {
				InteractionHook hook;
				if (interaction.getType() == InteractionType.COMPONENT) {
					hook = ((ButtonInteractionEvent) interaction).getHook();
				} else if (interaction.getType() == InteractionType.COMMAND) {
					hook = ((SlashCommandInteractionEvent) interaction).getHook();
				} else {
					throw new IllegalStateException("Unable to obtain Interaction Hook!");
				}
				Responses.info(hook, "Channel Unreserved", "Your channel has been unreserved.").queue();
				unreserveChannel(channel).queue();
				return;
			}
			potentialHelpers.sort((o1, o2) -> {
				int c = Integer.compare(participants.get(o1).size(), participants.get(o2).size());
				if (c == 0) return o1.getEffectiveName().compareTo(o2.getEffectiveName());
				return c;
			});
			sendThanksButtonsMessage(potentialHelpers, reservation, interaction, channel);
			try {
				setTimeout(channel, 5);
			} catch (SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
			}
		});
	}

	private void sendThanksButtonsMessage(@NotNull List<Member> potentialHelpers, ChannelReservation reservation, Interaction interaction, TextChannel channel) {
		List<ItemComponent> thanksButtons = new ArrayList<>(25);
		for (Member helper : potentialHelpers.subList(0, Math.min(potentialHelpers.size(), 20))) {
			thanksButtons.add(new ButtonImpl("help-thank:" + reservation.getId() + ":" + helper.getId(), helper.getEffectiveName(), ButtonStyle.SUCCESS, false, Emoji.fromUnicode("❤")));
		}
		ActionRow controlsRow = ActionRow.of(
				new ButtonImpl("help-thank:" + reservation.getId() + ":done", "Unreserve", ButtonStyle.PRIMARY, false, Emoji.fromUnicode("\u2705")),
				new ButtonImpl("help-thank:" + reservation.getId() + ":cancel", "Cancel", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("\u274C"))
		);
		InteractionHook hook;
		if (interaction.getType() == InteractionType.COMPONENT) {
			hook = ((ButtonInteractionEvent) interaction).getHook();
		} else if (interaction.getType() == InteractionType.COMMAND) {
			hook = ((SlashCommandInteractionEvent) interaction).getHook();
		} else {
			throw new IllegalStateException("Unable to obtain Interaction Hook!");
		}
		hook.sendMessage(THANK_MESSAGE_TEXT).setEphemeral(true).queue();
		List<ActionRow> rows = new ArrayList<>(5);
		rows.add(controlsRow);
		rows.addAll(MessageActionUtils.toActionRows(thanksButtons));
		channel.sendMessage(THANK_MESSAGE_TEXT).setComponents(rows).queue();
	}

	private void unreserveChannelByOtherUser(TextChannel channel, User owner, @Nullable String reason, SlashCommandInteractionEvent interaction) {
		if (reason != null) {// The user provided a reason, so check that it's legit, then send a DM to the owner.
			if (reason.isBlank() || reason.length() < 5) {
				Responses.warning(interaction.getHook(), "The reason you provided is not descriptive enough.").queue();
			} else {
				Responses.info(interaction.getHook(), "Channel Unreserved", "The channel has been unreserved.").queue();
				unreserveChannel(channel).queue();
				owner.openPrivateChannel().queue(pc -> pc.sendMessageFormat(
						"Your help channel **%s** has been unreserved by %s for the following reason:\n> %s",
						channel.getName(),
						interaction.getUser().getAsTag(),
						reason
				).queue());
			}
		} else {// No reason was provided, so just unreserve.
			Responses.info(interaction.getHook(), "Channel Unreserved", "The channel has been unreserved.").queue();
			unreserveChannel(channel).queue();
		}
	}

	/**
	 * Unreserves a channel after it no longer needs to be reserved.
	 *
	 * @param channel The channel to unreserve.
	 * @return A rest action that completes when everything is done.
	 */
	public RestAction<?> unreserveChannel(TextChannel channel) {
		if (this.config.isRecycleChannels()) {
			try (Connection con = dataSource.getConnection()) {
				Optional<ChannelReservation> reservationOptional = this.getReservationForChannel(channel.getIdLong());
				if (reservationOptional.isPresent()) {
					ChannelReservation reservation = reservationOptional.get();
					Map<Long, Double> experience = calculateExperience(HelpChannelListener.reservationMessages.get(reservation.getId()), reservation.getUserId(), config);
					for (Long recipient : experience.keySet()) {
						helpExperienceService.performTransaction(recipient, experience.get(recipient), channel.getGuild());
					}
				}
				try (PreparedStatement stmt = con.prepareStatement("DELETE FROM reserved_help_channels WHERE channel_id = ?")) {
					stmt.setLong(1, channel.getIdLong());
					stmt.executeUpdate();
					Category dormantCategory = config.getDormantChannelCategory();
					Category openCategory = config.getOpenChannelCategory();
					return RestAction.allOf(
							channel.retrievePinnedMessages()
									.flatMap(messages -> {
										if (messages.isEmpty()) {
											return new CompletedRestAction<>(channel.getJDA(), null);
										}
										return RestAction.allOf(messages.stream().map(Message::unpin).toList());
									}),
							getOpenChannelCount() >= config.getPreferredOpenChannelCount()
									? RestAction.allOf(channel.getManager().setParent(dormantCategory).sync(dormantCategory),
									channel.sendMessage(config.getDormantChannelMessage()))

									: RestAction.allOf(channel.getManager().setParent(openCategory).sync(openCategory),
									channel.sendMessage(config.getReopenedChannelMessage()))
					);
				}
			} catch (DataAccessException|SQLException e) {
				ExceptionLogger.capture(e, getClass().getSimpleName());
				return logChannel.sendMessage("Error occurred while unreserving help channel " + channel.getAsMention() + ": " + e.getMessage());
			}
		} else {
			return channel.delete();
		}
	}

	/**
	 * Unreserves all help channels the given user owns.
	 *
	 * @param user The user whose help channels should be unreserved.
	 * @throws SQLException If an error occurs.
	 */
	public void unreserveAllOwnedChannels(User user) throws SQLException {
		List<TextChannel> channels = dbActions.mapQuery(
				"SELECT channel_id FROM reserved_help_channels WHERE user_id = ?",
				s -> s.setLong(1, user.getIdLong()),
				rs -> {
					List<TextChannel> c = new ArrayList<>();
					while (rs.next()) {
						c.add(user.getJDA().getTextChannelById(rs.getLong(1)));
					}
					return c;
				}
		);
		for (TextChannel channel : channels) {
			unreserveChannel(channel);
		}
	}

	/**
	 * Tries to retrieve the current {@link ChannelReservation} by the channel id.
	 *
	 * @param channelId The channel's id.
	 * @return The {@link ChannelReservation} object as an {@link Optional}.
	 */
	public Optional<ChannelReservation> getReservationForChannel(long channelId) {
		return dbActions.fetchSingleEntity(
				"SELECT * FROM reserved_help_channels WHERE channel_id = ?",
				s -> s.setLong(1, channelId),
				rs -> new ChannelReservation(
						rs.getLong("id"),
						rs.getLong("channel_id"),
						rs.getTimestamp("reserved_at").toLocalDateTime(),
						rs.getLong("user_id"),
						rs.getInt("timeout")
				)
		);
	}

	/**
	 * Tries to retrieve the current {@link ChannelReservation} by its id.
	 *
	 * @param id The reservation's id.
	 * @return The {@link ChannelReservation} object as an {@link Optional}.
	 */
	public Optional<ChannelReservation> getReservation(long id) {
		return dbActions.fetchSingleEntity(
				"SELECT * FROM reserved_help_channels WHERE id = ?",
				s -> s.setLong(1, id),
				rs -> new ChannelReservation(
						rs.getLong("id"),
						rs.getLong("channel_id"),
						rs.getTimestamp("reserved_at").toLocalDateTime(),
						rs.getLong("user_id"),
						rs.getInt("timeout")
				)
		);
	}

	/**
	 * Sets the timeout for the given channel.
	 *
	 * @param channel The channel.
	 * @param timeout The timeout.
	 * @throws SQLException If an error occurs.
	 */
	public void setTimeout(@NotNull TextChannel channel, int timeout) throws SQLException {
		try (Connection con = dataSource.getConnection(); PreparedStatement stmt = con.prepareStatement("UPDATE reserved_help_channels SET timeout = ? WHERE channel_id = ?")) {
			stmt.setInt(1, timeout);
			stmt.setLong(2, channel.getIdLong());
			stmt.executeUpdate();
		}
	}

	/**
	 * Gets the given channel's timeout.
	 *
	 * @param channel The channel.
	 * @return The timout as an integer.
	 * @throws SQLException If an error occurs.
	 */
	public int getTimeout(TextChannel channel) throws SQLException {
		try (Connection con = dataSource.getConnection(); PreparedStatement stmt = con.prepareStatement("SELECT timeout FROM reserved_help_channels WHERE channel_id = ?")) {
			stmt.setLong(1, channel.getIdLong());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new SQLException("Could not get timeout for channel_id " + channel.getId());
			}
		}
	}

	/**
	 * Retrieves the time the given channel was reserved.
	 *
	 * @param channel The help channel.
	 * @return The time the given channel was reserved as a {@link LocalDateTime} object.
	 * @throws SQLException If an error occurs.
	 */
	public LocalDateTime getReservedAt(TextChannel channel) throws SQLException {
		return dbActions.mapQuery(
				"SELECT reserved_at FROM reserved_help_channels WHERE channel_id = ?",
				s -> s.setLong(1, channel.getIdLong()),
				rs -> {
					if (!rs.next()) throw new SQLException("No data!");
					return rs.getTimestamp(1).toLocalDateTime();
				}
		);
	}

	/**
	 * Calculates the next timeout for the given help channel.
	 *
	 * @param channel The help channel whose next timeout should be calculated.
	 * @return The next timeout as an integer.
	 * @throws SQLException If an error occurs.
	 */
	public int getNextTimeout(TextChannel channel) throws SQLException {
		if (config.getInactivityTimeouts().isEmpty()) {
			log.warn("No help channel inactivity timeouts have been configured!");
			return 60;
		}
		int currentTimeout = getTimeout(channel);
		int maxTimeout = config.getInactivityTimeouts().get(0);
		for (Integer t : config.getInactivityTimeouts()) {
			if (t > currentTimeout) {
				return t;
			}
			if (t > maxTimeout) maxTimeout = t;
		}
		return maxTimeout;
	}

	/**
	 * Gets the given help channel's reservation id.
	 *
	 * @param channel The channel whose id should be returned.
	 * @return The reservation id as an {@link Optional}.
	 */
	public Optional<Long> getReservationId(TextChannel channel) {
		try {
			return dbActions.mapQuery(
					"SELECT id FROM reserved_help_channels WHERE channel_id = ?",
					s -> s.setLong(1, channel.getIdLong()),
					rs -> {
						if (rs.next()) return Optional.of(rs.getLong(1));
						return Optional.empty();
					}
			);
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			return Optional.empty();
		}
	}

	/**
	 * Calculates the experience for each user, based on the messages they sent.
	 *
	 * @param messages The list of {@link Message}s.
	 * @param ownerId The owner id.
	 * @param config The {@link HelpConfig}, containing some static info for the calculation.
	 * @return A {@link Map}, containing the users' id as the key, and the amount of xp as the value.
	 */
	public static Map<Long, Double> calculateExperience(List<Message> messages, long ownerId, HelpConfig config) {
		Map<Long, Double> experience = new HashMap<>();
		if (messages == null || messages.isEmpty()) return Map.of();
		for (User user : messages.stream().map(Message::getAuthor).collect(Collectors.toSet())) {
			if (user.getIdLong() == ownerId) continue;
			int xp = 0;
			for (Message message : messages.stream()
					.filter(f -> f.getAuthor().getIdLong() != ownerId && f.getContentDisplay().length() > config.getMinimumMessageLength()).toList()) {
				xp += config.getBaseExperience() + config.getPerCharacterExperience() * (Math.log(message.getContentDisplay().trim().length()) / Math.log(2));
			}
			experience.put(user.getIdLong(), Math.min(xp, config.getMaxExperiencePerChannel()));
		}
		return experience;
	}
}
