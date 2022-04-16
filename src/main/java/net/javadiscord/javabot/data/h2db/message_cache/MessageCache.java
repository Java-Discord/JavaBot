package net.javadiscord.javabot.data.h2db.message_cache;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
import org.jetbrains.annotations.NotNull;

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
import java.util.Optional;

/**
 * Listens for Incoming Messages and stores them in the Message Cache.
 */
@Slf4j
public class MessageCache extends ListenerAdapter {
	List<CachedMessage> cache = new ArrayList<>();
	/**
	 * Amount of messages since the last synchronization.
	 * <p>
	 * If a certain threshold is reached, messages will be synchronized to reduce the chances of loosing
	 * messages during an unexpected shutdown.
	 */
	private int messageCount = 0;

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

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		MessageCacheConfig config = Bot.config.get(event.getGuild()).getMessageCache();
		if (cache.size() + 1 > config.getMaxCachedMessages()) {
			cache.remove(0);
		}
		if (messageCount >= config.getMessageSynchronizationInterval()) {
			synchronize();
		}
		messageCount++;
		cache.add(CachedMessage.of(event.getMessage()));
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		Optional<CachedMessage> optional = cache.stream().filter(m -> m.getMessageId() == event.getMessageIdLong()).findFirst();
		if (optional.isPresent()) {
			CachedMessage before = optional.get();
			if (event.getMessage().getContentRaw().trim().equals(before.getMessageContent())) return;
			MessageAction action = GuildUtils.getCacheLogChannel(event.getGuild())
					.sendMessageEmbeds(this.buildMessageEditEmbed(event.getGuild(), event.getAuthor(), event.getChannel(), before, event.getMessage()))
					.setActionRow(Button.link(event.getMessage().getJumpUrl(), "Jump to Message"));
			if (before.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH || event.getMessage().getContentRaw().length() > MessageEmbed.VALUE_MAX_LENGTH) {
				action.addFile(this.buildEditedMessageFile(event.getAuthor(), before, event.getMessage()), before.getMessageId() + ".txt");
			}
			action.queue();
			cache.set(cache.indexOf(before), CachedMessage.of(event.getMessage()));
		} else {
			GuildUtils.getCacheLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I could not retrieve its content.", event.getMessageIdLong())).queue();
		}
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		Optional<CachedMessage> optional = cache.stream().filter(m -> m.getMessageId() == event.getMessageIdLong()).findFirst();
		if (optional.isPresent()) {
			CachedMessage message = optional.get();
			event.getJDA().retrieveUserById(message.getAuthorId()).queue(author -> {
				MessageAction action = GuildUtils.getCacheLogChannel(event.getGuild())
						.sendMessageEmbeds(this.buildMessageDeleteEmbed(event.getGuild(), author, event.getChannel(), message));
				if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
					action.addFile(this.buildDeletedMessageFile(author, message), message.getMessageId() + ".txt");
				}
				action.queue();
			});
			cache.remove(message);
		} else {
			GuildUtils.getCacheLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I cannot retrieve its content.", event.getMessageIdLong())).queue();
		}
	}

	/**
	 * Checks whether the given message should be ignored by the cache.
	 *
	 * This is done with the following criteria:
	 * <ol>
	 *     <li>Message author is a bot</li>
	 *     <li>Message author is a system account</li>
	 *     <li>Message author is part of the excluded users</li>
	 *     <li>Channel is excluded from the cache</li>
	 * </ol>
	 *
	 * @param message The message to check
	 * @return true if any of the criteria above apply
	 */
	private boolean ignoreMessageCache(Message message) {
		MessageCacheConfig config = Bot.config.get(message.getGuild()).getMessageCache();
		return message.getAuthor().isBot() || message.getAuthor().isSystem() ||
				config.getExcludedUsers().contains(message.getAuthor().getIdLong()) ||
				config.getExcludedChannels().contains(message.getChannel().getIdLong());
	}

	private EmbedBuilder buildMessageCacheEmbed(User author, MessageChannel channel, CachedMessage before){
		long epoch = IdCalculatorCommand.getTimestampFromId(before.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.addField("Author", author.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.setFooter("ID: " + before.getMessageId());
	}

	private MessageEmbed buildMessageEditEmbed(Guild guild, User author, MessageChannel channel, CachedMessage before, Message after) {
		return buildMessageCacheEmbed(author, channel, before)
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
		return buildMessageCacheEmbed(author, channel, message)
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
