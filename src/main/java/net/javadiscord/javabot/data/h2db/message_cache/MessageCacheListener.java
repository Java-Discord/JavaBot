package net.javadiscord.javabot.data.h2db.message_cache;

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
import net.javadiscord.javabot.data.h2db.DbActions;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;

/**
 * Listens for Incoming Messages and stores them in the Message Cache.
 */
public class MessageCacheListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		MessageCacheConfig config = Bot.config.get(event.getGuild()).getMessageCache();
		if (DbActions.count("SELECT count(*) FROM message_cache") + 1 > config.getMaxCachedMessages()) {
			DbHelper.doDaoAction(MessageCacheRepository::new, dao -> dao.delete(dao.getLast().getMessageId()));
		}
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> dao.insert(CachedMessage.of(event.getMessage())));
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
			Optional<CachedMessage> optional = dao.getByMessageId(event.getMessageIdLong());
			if (optional.isPresent()) {
				CachedMessage before = optional.get();
				MessageAction action = GuildUtils.getLogChannel(event.getGuild())
						.sendMessageEmbeds(this.buildMessageEditEmbed(event.getGuild(), event.getAuthor(), event.getChannel(), before, event.getMessage()))
						.setActionRow(Button.link(event.getMessage().getJumpUrl(), "Jump to Message"));
				if (before.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH || event.getMessage().getContentRaw().length() > MessageEmbed.VALUE_MAX_LENGTH) {
					action.addFile(this.buildEditedMessageFile(event.getAuthor(), before, event.getMessage()), before.getMessageId() + ".txt");
				}
				action.queue();
				dao.update(CachedMessage.of(event.getMessage()));
			} else {
				GuildUtils.getLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I cannot retrieve its content.", event.getMessageIdLong())).queue();
			}
		});
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
			Optional<CachedMessage> optional = dao.getByMessageId(event.getMessageIdLong());
			if (optional.isPresent()) {
				CachedMessage message = optional.get();
				User author = event.getJDA().retrieveUserById(message.getAuthorId()).complete();
				MessageAction action = GuildUtils.getLogChannel(event.getGuild())
						.sendMessageEmbeds(this.buildMessageDeleteEmbed(event.getGuild(), author, event.getChannel(), message));
				if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
					action.addFile(this.buildDeletedMessageFile(author, message), message.getMessageId() + ".txt");
				}
				action.queue();
			} else {
				GuildUtils.getLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I cannot retrieve its content.", event.getMessageIdLong())).queue();
			}
			dao.delete(event.getMessageIdLong());
		});
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
