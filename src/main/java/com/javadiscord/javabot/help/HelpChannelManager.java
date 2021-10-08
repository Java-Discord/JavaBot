package com.javadiscord.javabot.help;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.properties.config.guild.HelpConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

import java.sql.SQLException;

/**
 * This manager is responsible for all the main interactions that affect the
 * help system's channels.
 */
@Slf4j
public class HelpChannelManager {
	private final HelpConfig config;
	private final TextChannel logChannel;

	public HelpChannelManager(HelpConfig config) {
		this.config = config;
		this.logChannel = Bot.config.get(config.getGuild()).getModeration().getLogChannel();
	}

	public boolean isOpen(TextChannel channel) {
		return config.getOpenChannelCategory().equals(channel.getParent());
	}

	public boolean isReserved(TextChannel channel) {
		return config.getReservedChannelCategory().equals(channel.getParent());
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
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("INSERT INTO reserved_help_channels (channel_id, user_id) VALUES (?, ?)");
			stmt.setLong(1, channel.getIdLong());
			stmt.setLong(2, reservingUser.getIdLong());
			stmt.executeUpdate();
		}
		var target = config.getReservedChannelCategory();
		channel.getManager().setParent(target).sync(target).queue();
		// Pin the message, then immediately try and delete the annoying "message has been pinned" message.
		message.pin().queue(unused -> channel.getHistory().retrievePast(10).queue(messages -> {
			for (var msg : messages) {
				if (msg.getType().equals(MessageType.CHANNEL_PINNED_ADD)) {
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
		User user = null;
		try (var con = Bot.dataSource.getConnection()) {
			var stmt = con.prepareStatement("SELECT * FROM reserved_help_channels WHERE channel_id = ?");
			stmt.setLong(1, channel.getIdLong());
			var rs = stmt.executeQuery();
			if (rs.next()) {
				long userId = rs.getLong("user_id");
				user = channel.getJDA().retrieveUserById(userId).complete();
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logChannel.sendMessage("Error occurred while getting reserved channel owner: " + e.getMessage()).queue();
		}
		return user;
	}

	/**
	 * Unreserves a channel after it no longer needs to be reserved.
	 * @param channel The channel to unreserve.
	 */
	public RestAction<?> unreserveChannel(TextChannel channel) {
		if (this.config.isRecycleChannels()) {
			try (var con = Bot.dataSource.getConnection()) {
				var stmt = con.prepareStatement("DELETE FROM reserved_help_channels WHERE channel_id = ?");
				stmt.setLong(1, channel.getIdLong());
				stmt.executeUpdate();
				return RestAction.allOf(
						channel.retrievePinnedMessages()
								.flatMap(messages -> RestAction.allOf(messages.stream().map(Message::unpin).toList())),
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
}
