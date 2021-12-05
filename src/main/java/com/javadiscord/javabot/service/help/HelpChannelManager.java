package com.javadiscord.javabot.service.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.data.h2db.DbActions;
import com.javadiscord.javabot.data.properties.config.guild.HelpConfig;
import com.javadiscord.javabot.service.help.model.ChannelReservation;
import com.javadiscord.javabot.utils.MessageActionUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.internal.interactions.ButtonImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;

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
	public static final String THANK_MESSAGE_TEXT = "Before your channel will be unreserved, would you like to express your gratitude to any of the people who helped you? When you're done, click **Unreserve**.";

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
	 * @param user The user who is trying to reserve a channel.
	 * @return True if the user can reserve it, or false if not.
	 */
	public boolean mayUserReserveChannel(User user) {
		var member = this.config.getGuild().getMember(user);
		// Only allow guild members.
		if (member == null) return false;
		// Don't allow muted users.
		if (member.getRoles().contains(Bot.config.get(this.config.getGuild()).getModeration().getMuteRole())) return false;
		try (var con = Bot.dataSource.getConnection();
				var stmt = con.prepareStatement("SELECT COUNT(channel_id) FROM reserved_help_channels WHERE user_id = ?")) {
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
		if (category == null) throw new IllegalStateException("Missing help channel category. Cannot open a new help channel.");
		String name = this.config.getChannelNamingStrategy().getName(category.getTextChannels(), config);
		category.createTextChannel(name).queue(channel -> {
			channel.getManager().setPosition(0).setTopic(this.config.getOpenChannelTopic()).queue();
			log.info("Created new help channel {}.", channel.getAsMention());
		});
	}

	/**
	 * Reserves a text channel for a user.
	 * @param channel The channel to reserve.
	 * @param reservingUser The user who is reserving the channel.
	 * @param message The message the user sent in the channel.
	 */
	public void reserve(TextChannel channel, User reservingUser, Message message) throws SQLException {
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
				dormantChannels.get(0).getManager().setParent(targetCategory).sync(targetCategory).queue();
			} else {
				logChannel.sendMessage("Warning: No dormant channels were available to replenish an open channel that was just reserved.").queue();
			}
		} else {
			this.openNew();
		}
	}

	/**
	 * Gets the owner of a reserved channel.
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
	 * @param channel The channel to unreserve.
	 * @param owner The owner of the reserved channel.
	 * @param reason The user-supplied reason for unreserving the channel.
	 * @param interaction The interaction the user did to unreserve the channel.
	 */
	public void unreserveChannelByUser(TextChannel channel, User owner, @Nullable String reason, Interaction interaction) {
		if (owner.equals(interaction.getUser())) {// The user is unreserving their own channel.
			unreserveChannelByOwner(channel, owner, interaction);
		} else {// The channel was unreserved by someone other than the owner.
			unreserveChannelByOtherUser(channel, owner, reason, interaction);
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
				Responses.info(interaction.getHook(), "Channel Unreserved", "Your channel has been unreserved.").queue();
				unreserveChannel(channel).queue();
				return;
			}
			potentialHelpers.sort((o1, o2) -> {
				int c = Integer.compare(participants.get(o1).size(), participants.get(o2).size());
				if (c == 0) return o1.getEffectiveName().compareTo(o2.getEffectiveName());
				return c;
			});
			List<Component> components = new ArrayList<>(25);
			for (var helper : potentialHelpers.subList(0, Math.min(potentialHelpers.size(), 23))) {
				components.add(new ButtonImpl("help-thank:" + reservation.getId() + ":" + helper.getId(), helper.getEffectiveName(), ButtonStyle.SUCCESS, false, Emoji.fromUnicode("❤")));
			}
			components.add(new ButtonImpl("help-thank:" + reservation.getId() + ":done", "Unreserve", ButtonStyle.PRIMARY, false, null));
			components.add(new ButtonImpl("help-thank:" + reservation.getId() + ":cancel", "Cancel", ButtonStyle.SECONDARY, false, Emoji.fromUnicode("❌")));
			interaction.getHook().sendMessage("Before your channel is unreserved, we would appreciate if you could take a moment to acknowledge those who helped you. This helps us to reward users who contribute to helping others, and gives us better insight into how to help users more effectively. Otherwise, click the **Unreserve** button simply unreserve your channel.")
					.setEphemeral(true).queue();
			var msgAction = channel.sendMessage(THANK_MESSAGE_TEXT);
			msgAction = MessageActionUtils.addComponents(msgAction, components);
			msgAction.queue();
			try {
				setTimeout(channel, 5);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});
	}

	private void unreserveChannelByOtherUser(TextChannel channel, User owner, @Nullable String reason, Interaction interaction) {
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
	 * @param channel The channel to unreserve.
	 * @return A rest action that completes when everything is done.
	 */
	public RestAction<?> unreserveChannel(TextChannel channel) {
		if (this.config.isRecycleChannels()) {
			try (var con = Bot.dataSource.getConnection();
					var stmt = con.prepareStatement("DELETE FROM reserved_help_channels WHERE channel_id = ?")) {
				stmt.setLong(1, channel.getIdLong());
				stmt.executeUpdate();
				return RestAction.allOf(
						channel.retrievePinnedMessages()
								.flatMap(messages -> {
									if (messages.isEmpty()) return new CompletedRestAction<>(channel.getJDA(), null);
									return RestAction.allOf(messages.stream().map(Message::unpin).toList());
								}),
						getOpenChannelCount() >= config.getPreferredOpenChannelCount()
								? channel.getManager().setParent(config.getDormantChannelCategory()).sync(config.getDormantChannelCategory())
								: channel.getManager().setParent(config.getOpenChannelCategory()).sync(config.getOpenChannelCategory()),
						channel.sendMessage(this.config.getReopenedChannelMessage())
				);
			} catch (SQLException e) {
				e.printStackTrace();
				return logChannel.sendMessage("Error occurred while unreserving help channel " + channel.getAsMention() + ": " + e.getMessage());
			}
		} else {
			return channel.delete();
		}
	}

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

	public void setTimeout(TextChannel channel, int timeout) throws SQLException {
		try (var con = Bot.dataSource.getConnection();
			 var stmt = con.prepareStatement("UPDATE reserved_help_channels SET timeout = ? WHERE channel_id = ?")
		) {
			stmt.setInt(1, timeout);
			stmt.setLong(2, channel.getIdLong());
			stmt.executeUpdate();
		}
	}

	public int getTimeout(TextChannel channel) throws SQLException {
		try (var con = Bot.dataSource.getConnection();
			var stmt = con.prepareStatement("SELECT timeout FROM reserved_help_channels WHERE channel_id = ?")
		) {
			stmt.setLong(1, channel.getIdLong());
			var rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new SQLException("Could not get timeout for channel_id " + channel.getId());
			}
		}
	}

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
