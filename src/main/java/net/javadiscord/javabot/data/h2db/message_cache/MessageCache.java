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
import net.javadiscord.javabot.data.h2db.message_cache.dao.MessageCacheRepository;
import net.javadiscord.javabot.data.h2db.message_cache.model.CachedMessage;
import net.javadiscord.javabot.systems.commands.IdCalculatorCommand;
import net.javadiscord.javabot.util.GuildUtils;
import net.javadiscord.javabot.util.TimeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
     *
     * If a certain threshold is reached, messages will be synchronized to reduce the chances of loosing
     * messages during an unexpected shutdown.
     */
    int messageCount = 0;

	public MessageCache() {
		try {
			cache = new MessageCacheRepository(Bot.dataSource.getConnection()).getAll();
		} catch (SQLException e) {
			log.error("Something went wrong during retrieval of stored messages.");
		}
	}

	/**
	 * Synchronizes Messages saved in the Database with what is currently stored in memory.
	 */
	public void synchronize() {
		try {
			new MessageCacheRepository(Bot.dataSource.getConnection()).delete(cache.size());
			new MessageCacheRepository(Bot.dataSource.getConnection()).insertList(cache);
            messageCount = 0;
			log.info("Synchronized Database with local Cache.");
		} catch (SQLException e) {
			log.error("Something went wrong during synchronization of messages with DB.");
			log.error(e.getMessage());
		}
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		MessageCacheConfig config = Bot.config.get(event.getGuild()).getMessageCache();
		if (cache.size() + 1 > config.getMaxCachedMessages()) {
			cache.remove(0);
		}
        if (messageCount >= 50) {
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
		System.out.println(cache.toString());
		Optional<CachedMessage> optional = cache.stream().filter(m -> m.getMessageId() == event.getMessageIdLong()).findFirst();
		if (optional.isPresent()) {
			CachedMessage message = optional.get();
			User author = event.getJDA().retrieveUserById(message.getAuthorId()).complete();
			MessageAction action = GuildUtils.getCacheLogChannel(event.getGuild())
					.sendMessageEmbeds(this.buildMessageDeleteEmbed(event.getGuild(), author, event.getChannel(), message));
			if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
				action.addFile(this.buildDeletedMessageFile(author, message), message.getMessageId() + ".txt");
			}
			action.queue();
			cache.remove(message);
		} else {
			GuildUtils.getCacheLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I cannot retrieve its content.", event.getMessageIdLong())).queue();
		}
	}

	private boolean ignoreMessageCache(Message message) {
		MessageCacheConfig config = Bot.config.get(message.getGuild()).getMessageCache();
		return message.getAuthor().isBot() || message.getAuthor().isSystem() ||
				Arrays.asList(config.getExcludedUsers()).contains(message.getAuthor().getIdLong()) ||
				Arrays.asList(config.getExcludedChannels()).contains(message.getChannel().getIdLong());
	}

	private MessageEmbed buildMessageEditEmbed(Guild guild, User author, MessageChannel channel, CachedMessage before, Message after) {
		long epoch = IdCalculatorCommand.getTimestampFromId(before.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Edited")
				.setColor(Bot.config.get(guild).getSlashCommand().getWarningColor())
				.addField("Author", author.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.addField("Before", before.getMessageContent().substring(0, Math.min(
						before.getMessageContent().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false)
				.addField("After", after.getContentRaw().substring(0, Math.min(
						after.getContentRaw().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false)
				.setFooter("ID: " + before.getMessageId())
				.build();
	}

	private MessageEmbed buildMessageDeleteEmbed(Guild guild, User author, MessageChannel channel, CachedMessage message) {
		long epoch = IdCalculatorCommand.getTimestampFromId(message.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Deleted")
				.setColor(Bot.config.get(guild).getSlashCommand().getErrorColor())
				.addField("Author", author.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.addField("Message Content",
						message.getMessageContent().substring(0, Math.min(
								message.getMessageContent().length(),
								MessageEmbed.VALUE_MAX_LENGTH)), false)
				.setFooter("ID: " + message.getMessageId())
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
