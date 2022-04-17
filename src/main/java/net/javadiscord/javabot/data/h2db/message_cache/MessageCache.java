package net.javadiscord.javabot.data.h2db.message_cache;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.MessageCacheConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.data.h2db.message_cache.dao.MessageCacheRepository;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;
import net.javadiscord.javabot.systems.commands.IdCalculatorCommand;
import net.javadiscord.javabot.util.GuildUtils;
import net.javadiscord.javabot.util.TimeUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Listens for Incoming Messages and stores them in the Message Cache.
 */
@Slf4j
public class MessageCache {
	/**
	 * A memory-cache (list) of sent Messages, wrapped to a {@link CachedMessage} object.
	 */
	public List<CachedMessage> cache = new ArrayList<>();
	/**
	 * Amount of messages since the last synchronization.
	 * <p>
	 * If a certain threshold is reached, messages will be synchronized to reduce the chances of loosing
	 * messages during an unexpected shutdown.
	 */
	public int messageCount = 0;

	/**
	 * Creates a new messages & loads messages from the DB into a List.
	 */
	public MessageCache() {
		try (Connection con = Bot.dataSource.getConnection()) {
			cache = new MessageCacheRepository(con).getAll();
		} catch (SQLException e) {
			log.error("Something went wrong during retrieval of stored messages.");
		}
	}

	/**
	 * Synchronizes Messages saved in the Database with what is currently stored in memory.
	 */
	public void synchronize() {
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
			dao.delete(cache.size());
			dao.insertList(cache);
			messageCount = 0;
			log.info("Synchronized Database with local Cache.");
		});
	}

	/**
	 * Caches a single {@link Message} object.
	 *
	 * @param message The message to cache.
	 */
	public void cache(Message message) {
		MessageCacheConfig config = Bot.config.get(message.getGuild()).getMessageCache();
		if (cache.size() + 1 > config.getMaxCachedMessages()) {
			cache.remove(0);
		}
		if (messageCount >= config.getMessageSynchronizationInterval()) {
			synchronize();
		}
		messageCount++;
		cache.add(CachedMessage.of(message));
	}

	/**
	 * Sends the updated message's content to the {@link MessageCacheConfig#getMessageCacheLogChannel()}.
	 *
	 * @param updated The new {@link Message}.
	 * @param before  The {@link CachedMessage}.
	 */
	public void sendUpdatedMessageToLog(Message updated, CachedMessage before) {
		if (updated.getContentRaw().trim().equals(before.getMessageContent())) return;
		MessageAction action = GuildUtils.getCacheLogChannel(updated.getGuild())
				.sendMessageEmbeds(this.buildMessageEditEmbed(updated.getGuild(), updated.getAuthor(), updated.getChannel(), before, updated))
				.setActionRow(Button.link(updated.getJumpUrl(), "Jump to Message"));
		if (before.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH || updated.getContentRaw().length() > MessageEmbed.VALUE_MAX_LENGTH) {
			action.addFile(this.buildEditedMessageFile(updated.getAuthor(), before, updated), before.getMessageId() + ".txt");
		}
		action.queue();
	}

	/**
	 * Sends the deleted message's content to the {@link MessageCacheConfig#getMessageCacheLogChannel()}.
	 *
	 * @param guild   The message's {@link Guild}.
	 * @param channel The message's {@link MessageChannel}.
	 * @param message The {@link CachedMessage}.
	 */
	public void sendDeletedMessageToLog(Guild guild, MessageChannel channel, CachedMessage message) {
		guild.getJDA().retrieveUserById(message.getAuthorId()).queue(author -> {
			MessageAction action = GuildUtils.getCacheLogChannel(guild)
					.sendMessageEmbeds(this.buildMessageDeleteEmbed(guild, author, channel, message));
			if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
				action.addFile(this.buildDeletedMessageFile(author, message), message.getMessageId() + ".txt");
			}
			action.queue();
		});
	}

	private EmbedBuilder buildMessageCacheEmbed(MessageChannel channel, User author, CachedMessage before) {
		long epoch = IdCalculatorCommand.getTimestampFromId(before.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.addField("Author", author.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.setFooter("ID: " + before.getMessageId());
	}

	private MessageEmbed buildMessageEditEmbed(Guild guild, User author, MessageChannel channel, CachedMessage before, Message after) {
		return buildMessageCacheEmbed(channel, author, before)
				.setTitle("Message Edited")
				.setColor(Bot.config.get(guild).getSlashCommand().getWarningColor())
				.addField("Before", before.getMessageContent().substring(0, Math.min(
						before.getMessageContent().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false)
				.addField("After", after.getContentRaw().substring(0, Math.min(
						after.getContentRaw().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false)
				.build();
	}

	private MessageEmbed buildMessageDeleteEmbed(Guild guild, User author, MessageChannel channel, CachedMessage message) {
		return buildMessageCacheEmbed(channel, author, message)
				.setTitle("Message Deleted")
				.setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
				.addField("Message Content",
						message.getMessageContent().substring(0, Math.min(
								message.getMessageContent().length(),
								MessageEmbed.VALUE_MAX_LENGTH)), false)
				.build();
	}

	private InputStream buildDeletedMessageFile(User author, CachedMessage message) {
		DateTimeFormatter formatter = TimeUtils.STANDARD_FORMATTER.withZone(ZoneOffset.UTC);
		Instant instant = Instant.ofEpochMilli(IdCalculatorCommand.getTimestampFromId(message.getMessageId()));
		String in = String.format("""
				Author: %s
				ID: %s
				Created at: %s
								
				--- Message Content ---
								
				%s
				""", author.getAsTag(), message.getMessageId(), formatter.format(instant), message.getMessageContent());
		return new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
	}

	private InputStream buildEditedMessageFile(User author, CachedMessage before, Message after) {
		DateTimeFormatter formatter = TimeUtils.STANDARD_FORMATTER.withZone(ZoneOffset.UTC);
		Instant instant = Instant.ofEpochMilli(IdCalculatorCommand.getTimestampFromId(before.getMessageId()));
		String in = String.format("""
				Author: %s
				ID: %s
				Created at: %s
								
				--- Message Content (before) ---
								
				%s
								
				--- Message Content (after) ---
								
				%s
				""", author.getAsTag(), before.getMessageId(), formatter.format(instant), before.getMessageContent(), after.getContentRaw());
		return new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
	}
}
