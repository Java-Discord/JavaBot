package net.javadiscord.javabot.data.h2db.message_cache;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.javadiscord.javabot.Bot;
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
import java.util.Optional;

/**
 * Listens for Incoming Messages and stores them in the Message Cache.
 */
public class MessageCacheListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!this.shouldBeCached(event.getMessage())) return;
		if (DbActions.count("SELECT count(*) FROM message_cache") + 1 > Bot.config.get(event.getGuild()).getModeration().getMaxCachedMessages()) {
			DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
				dao.delete(dao.getLast().getMessageId());
			});
		}
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
			dao.insert(MessageCacheRepository.toCachedMessage(event.getMessage()));
		});
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (!this.shouldBeCached(event.getMessage())) return;
		DbHelper.doDaoAction(MessageCacheRepository::new, dao -> {
			dao.update(MessageCacheRepository.toCachedMessage(event.getMessage()));
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
						.sendMessageEmbeds(this.buildDeletedMessageEmbed(event.getGuild(), author, message));
				if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
					action.addFile(this.buildCachedMessageFile(author, message), message.getMessageId() + ".txt");
				}
				action.queue();
			} else {
				GuildUtils.getLogChannel(event.getGuild()).sendMessage(String.format("Message `%s` was not cached, thus, I cannot retrieve its content.", event.getMessageIdLong())).queue();
			}
			dao.delete(event.getMessageIdLong());
		});
	}

	private MessageEmbed buildDeletedMessageEmbed(Guild guild, User author, CachedMessage message) {
		long epoch = IdCalculatorCommand.getUnixTimestampFromSnowflakeId(message.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Message Deleted")
				.setColor(Bot.config.get(guild).getSlashCommand().getWarningColor())
				.addField("Author", author.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.addField("Message Content",
						message.getMessageContent().substring(0, Math.min(
								message.getMessageContent().length(),
								MessageEmbed.VALUE_MAX_LENGTH)), false)
				.setFooter("ID: " + message.getMessageId())
				.build();
	}

	private boolean shouldBeCached(Message message) {
		return !message.getAuthor().isBot() && !message.getAuthor().isSystem() && message.getContentRaw().length() > 0;
	}

	private InputStream buildCachedMessageFile(User author, CachedMessage message) {
		DateTimeFormatter formatter = TimeUtils.STANDARD_FORMATTER.withZone(ZoneOffset.UTC);
		Instant instant = Instant.ofEpochMilli(IdCalculatorCommand.getUnixTimestampFromSnowflakeId(message.getMessageId()));
		String in = String.format("""
				Author: %s
				ID: %s
				Created at: %s
				
				--- Message Content ---
				
				%s
				""", author.getAsTag(), message.getMessageId(), formatter.format(instant), message.getMessageContent());
		return new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
	}
}
