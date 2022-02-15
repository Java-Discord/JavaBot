package net.javadiscord.javabot.systems.help;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
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
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;
import net.javadiscord.javabot.util.MessageActionUtils;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
	private final TextChannel logChannel;

	public HelpChannelManager(HelpConfig config) {
		this.config = config;
		this.logChannel = Bot.config.get(config.getGuild()).getModeration().getLogChannel();
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
		var member = this.config.getGuild().getMember(user);
		// Only allow guild members.
		if (member == null) return false;
		// Don't allow muted users.
		if (member.isTimedOut()) return false;
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("SELECT COUNT(channel_id) FROM reserved_help_channels WHERE user_id = ?");
			stmt.setLong(1, user.getIdLong());
			var rs = stmt.executeQuery();
			return rs.next() && rs.getLong(1) < this.config.getMaxReservedChannelsPerUser();
		} catch (SQLException e) {
			e.printStackTrace();
			logChannel.sendMessage("Error while checking if a user can reserve a help channel: " + e.getMessage()).queue();
			return false;
		}
	}

	/**
	 * Opens a text channel so that it is ready for a new question.
	 */
	public void openNew() {
		var category = config.getOpenChannelCategory();
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
		var alreadyReservedCount = DbActions.count(
				"SELECT COUNT(id) FROM reserved_help_channels WHERE channel_id = ?",
				s -> s.setLong(1, channel.getIdLong())
		);
		// If it's marked in the DB as reserved, remove that so that we can reserve it anew.
		if (alreadyReservedCount > 0) {
			DbActions.update("DELETE FROM reserved_help_channels WHERE channel_id = ?", channel.getIdLong());
		}
		int timeout = config.getInactivityTimeouts().get(0);
		DbActions.update(
				"INSERT INTO reserved_help_channels (channel_id, user_id, timeout) VALUES (?, ?, ?)",
				channel.getIdLong(), reservingUser.getIdLong(), timeout
		);
		var target = config.getReservedChannelCategory();
		channel.getManager().setParent(target).sync(target).queue();
		// Pin the message, then immediately try and delete the annoying "message has been pinned" message.
		message.pin().queue(unused -> channel.getHistory().retrievePast(10).queue(messages -> {
			for (var msg : messages) {
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
			var dormantChannels = this.config.getDormantChannelCategory().getTextChannels();
			if (!dormantChannels.isEmpty()) {
				var targetCategory = this.config.getOpenChannelCategory();
				var targetChannel = dormantChannels.get(0);
				targetChannel.getManager().setParent(targetCategory).sync(targetCategory).queue();
				targetChannel.sendMessage(config.getReopenedChannelMessage()).queue();
			} else {
				logChannel.sendMessage("Warning: No dormant channels were available to replenish an open channel that was just reserved.").queue();
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
			return DbActions.mapQuery(
					"SELECT user_id FROM reserved_help_channels WHERE channel_id = ?",
					s -> s.setLong(1, channel.getIdLong()),
					rs -> {
						if (rs.next()) return channel.getJDA().retrieveUserById(rs.getLong(1)).complete();
						return null;
					}
			);
		} catch (SQLException e) {
			e.printStackTrace();
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
	public CompletableFuture<Map<Member, List<Message>>> getParticipantsSinceReserved(TextChannel channel) {
		int limit = 300;
		var history = channel.getHistory();
		final CompletableFuture<Map<Member, List<Message>>> cf = new CompletableFuture<>();
		Bot.asyncPool.execute(() -> {
			final Map<Member, List<Message>> userMessages = new HashMap<>();
			boolean endFound = false;
			while (!endFound && history.size() < limit) {
				var messages = history.retrievePast(50).complete();
				for (Message msg : messages) {
					if (msg.getContentRaw().contains(config.getReservedChannelMessage()) || msg.isPinned()) {
						endFound = true;
						break;
					}
					var user = msg.getAuthor();
					if (!user.isBot() && !user.isSystem()) {
						var member = channel.getGuild().retrieveMember(user).complete();
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
	public void unreserveChannelByUser(TextChannel channel, User owner, @Nullable String reason, Interaction interaction) {
		if (owner.equals(interaction.getUser())) {// The user is unreserving their own channel.
			unreserveChannelByOwner(channel, owner, interaction);
		} else {// The channel was unreserved by someone other than the owner.
			unreserveChannelByOtherUser(channel, owner, reason, (SlashCommandInteractionEvent) interaction);
		}
	}

	private void unreserveChannelByOwner(TextChannel channel, User owner, Interaction interaction) {
		var optionalReservation = getReservationForChannel(channel.getIdLong());
		if (optionalReservation.isEmpty()) {
			log.warn("Could not find current reservation data for channel {}. Unreserving the channel without thanks.", channel.getAsMention());
			unreserveChannel(channel);
			return;
		}
		var reservation = optionalReservation.get();
		// Ask the user for some feedback about the help channel, if possible.
		getParticipantsSinceReserved(channel).thenAcceptAsync(participants -> {
			List<Member> potentialHelpers = new ArrayList<>(participants.size());
			for (var entry : participants.entrySet()) {
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
				e.printStackTrace();
			}
		});
	}

	private void sendThanksButtonsMessage(List<Member> potentialHelpers, ChannelReservation reservation, Interaction interaction, TextChannel channel) {
		List<ItemComponent> thanksButtons = new ArrayList<>(25);
		for (var helper : potentialHelpers.subList(0, Math.min(potentialHelpers.size(), 20))) {
			thanksButtons.add(new ButtonImpl("help-thank:" + reservation.getId() + ":" + helper.getId(), helper.getEffectiveName(), ButtonStyle.SUCCESS, false, Emoji.fromUnicode("❤")));
		}
		ActionRow controlsRow = ActionRow.of(
				new ButtonImpl("help-thank:" + reservation.getId() + ":done", "Unreserve", ButtonStyle.PRIMARY, false, Emoji.fromUnicode("✅")),
				new ButtonImpl("help-thank:" + reservation.getId() + ":cancel", "Cancel", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("❌"))
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
		channel.sendMessage(THANK_MESSAGE_TEXT).setActionRows(rows).queue();
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
			try (var con = Bot.dataSource.getConnection()) {
				var stmt = con.prepareStatement("DELETE FROM reserved_help_channels WHERE channel_id = ?");
				stmt.setLong(1, channel.getIdLong());
				stmt.executeUpdate();
				var dormantCategory = config.getDormantChannelCategory();
				var openCategory = config.getOpenChannelCategory();
				return RestAction.allOf(
						channel.retrievePinnedMessages()
								.flatMap(messages -> {
									if (messages.isEmpty()) return new CompletedRestAction<>(channel.getJDA(), null);
									return RestAction.allOf(messages.stream().map(Message::unpin).toList());
								}),
						getOpenChannelCount() >= config.getPreferredOpenChannelCount()
								? RestAction.allOf(channel.getManager().setParent(dormantCategory).sync(dormantCategory),
								channel.sendMessage(config.getDormantChannelMessage()))

								: RestAction.allOf(channel.getManager().setParent(openCategory).sync(openCategory),
								channel.sendMessage(config.getReopenedChannelMessage()))
				);
			} catch (SQLException e) {
				e.printStackTrace();
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
		var channels = DbActions.mapQuery(
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
		for (var channel : channels) {
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
		return DbActions.fetchSingleEntity(
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
		return DbActions.fetchSingleEntity(
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
	public void setTimeout(TextChannel channel, int timeout) throws SQLException {
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("UPDATE reserved_help_channels SET timeout = ? WHERE channel_id = ?");
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
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("SELECT timeout FROM reserved_help_channels WHERE channel_id = ?");
			stmt.setLong(1, channel.getIdLong());
			var rs = stmt.executeQuery();
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
		return DbActions.mapQuery(
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
		for (var t : config.getInactivityTimeouts()) {
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
			return DbActions.mapQuery(
					"SELECT id FROM reserved_help_channels WHERE channel_id = ?",
					s -> s.setLong(1, channel.getIdLong()),
					rs -> {
						if (rs.next()) return Optional.of(rs.getLong(1));
						return Optional.empty();
					}
			);
		} catch (SQLException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
