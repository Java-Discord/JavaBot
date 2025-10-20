package net.discordjug.javabot.data.h2db.message_cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.MessageCacheConfig;
import net.discordjug.javabot.data.h2db.message_cache.dao.MessageCacheRepository;
import net.discordjug.javabot.data.h2db.message_cache.model.CachedMessage;
import net.discordjug.javabot.systems.user_commands.IdCalculatorCommand;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.TimeUtils;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Listens for Incoming Messages and stores them in the Message Cache.
 */
@Slf4j
@Service
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
	@Getter
	private int messageCount = 0;

	private final ExecutorService asyncPool;
	private final BotConfig botConfig;
	private final MessageCacheRepository cacheRepository;

	/**
	 * Creates a new messages & loads messages from the DB into a List.
	 * @param botConfig The main configuration of the bot
	 * @param cacheRepository Dao class that represents the QOTW_POINTS SQL Table.
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public MessageCache(BotConfig botConfig, MessageCacheRepository cacheRepository, ExecutorService asyncPool) {
		this.asyncPool = asyncPool;
		this.botConfig = botConfig;
		this.cacheRepository = cacheRepository;
		try {
			cache = cacheRepository.getAll();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			log.error("Something went wrong during retrieval of stored messages.");
		}

	}

	/**
	 * Synchronizes Messages saved in the Database with what is currently stored in memory.
	 */
	public void synchronize() {
		asyncPool.execute(()->{
			cacheRepository.delete(cache.size());
			cacheRepository.insertList(cache);
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
		MessageCacheConfig config = botConfig.get(message.getGuild()).getMessageCacheConfig();
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
		MessageCacheConfig config = botConfig.get(updated.getGuild()).getMessageCacheConfig();
		if (config.getMessageCacheLogChannel() == null) return;
		if (updated.getContentRaw().trim().equals(before.getMessageContent()) && updated.getAttachments().size() == before.getAttachments().size()) return;
		MessageCreateAction action = config.getMessageCacheLogChannel()
				.sendMessageEmbeds(buildMessageEditEmbed(updated.getGuild(), updated.getAuthor(), updated.getChannel(), before, updated))
				.addComponents(ActionRow.of(Button.link(updated.getJumpUrl(), "Jump to Message")));
		if (before.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH || updated.getContentRaw().length() > MessageEmbed.VALUE_MAX_LENGTH) {
			action.addFiles(FileUpload.fromData(buildEditedMessageFile(updated.getAuthor(), before, updated), before.getMessageId() + ".txt"));
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
		MessageCacheConfig config = botConfig.get(guild).getMessageCacheConfig();
		if (config.getMessageCacheLogChannel() == null) return;
		guild.getJDA().retrieveUserById(message.getAuthorId()).queue(author -> {
			MessageCreateAction action = config.getMessageCacheLogChannel().sendMessageEmbeds(buildMessageDeleteEmbed(guild, author, channel, message));
			if (message.getMessageContent().length() > MessageEmbed.VALUE_MAX_LENGTH) {
				action.addFiles(FileUpload.fromData(buildDeletedMessageFile(author, message), message.getMessageId() + ".txt"));
			}
			action.queue();
			requestMessageAttachments(message);
		});
	}

	/**
	 * Requests each attachment from Discord's CDN.
	 * This is done in order to prevent Discord from deleting the attachment too quickly.
	 * @param message the cached message
	 */
	private void requestMessageAttachments(CachedMessage message) {
		HttpClient client = HttpClient.newHttpClient();
		for (String attachment : message.getAttachments()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(attachment)).build();
			client.sendAsync(request, BodyHandlers.discarding());
		}
	}

	private EmbedBuilder buildMessageCacheEmbed(MessageChannel channel, User author, CachedMessage before) {
		long epoch = IdCalculatorCommand.getTimestampFromId(before.getMessageId()) / 1000;
		return new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(author), null, author.getEffectiveAvatarUrl())
				.addField("Author", author.getAsMention(), true)
				.addField("Channel", channel.getAsMention(), true)
				.addField("Created at", String.format("<t:%s:F>", epoch), true)
				.setFooter("ID: " + before.getMessageId());
	}

	private MessageEmbed buildMessageEditEmbed(Guild guild, User author, MessageChannel channel, CachedMessage before, Message after) {
		EmbedBuilder eb = buildMessageCacheEmbed(channel, author, before)
				.setTitle("Message Edited")
				.setColor(Responses.Type.WARN.getColor())
				.addField("Before", before.getMessageContent().substring(0, Math.min(
						before.getMessageContent().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false)
				.addField("After", after.getContentRaw().substring(0, Math.min(
						after.getContentRaw().length(),
						MessageEmbed.VALUE_MAX_LENGTH)), false);
		if(before.getAttachments().size() != after.getAttachments().size()) {
			eb.addField("Deleted Attachments",
					before
						.getAttachments()
						.stream()
						.filter(attachment -> after//not present in 'after'
								.getAttachments()
								.stream()
								.map(Attachment::getUrl)
								.noneMatch(attachment::equals))
						.collect(Collectors.joining("\n")),
						false);
		}
		return eb
				.build();
	}

	private MessageEmbed buildMessageDeleteEmbed(Guild guild, User author, MessageChannel channel, CachedMessage message) {
		EmbedBuilder eb = buildMessageCacheEmbed(channel, author, message)
				.setTitle("Message Deleted")
				.setColor(Responses.Type.ERROR.getColor())
				.addField("Message Content",
						message.getMessageContent().substring(0, Math.min(
								message.getMessageContent().length(),
								MessageEmbed.VALUE_MAX_LENGTH)), false);
		if (!message.getAttachments().isEmpty()) {
			addAttachmentsToMessageBuilder(message, eb);
		}
		return eb.build();
	}

	private void addAttachmentsToMessageBuilder(CachedMessage message, EmbedBuilder eb) {
		StringBuilder attachmentBuilder = new StringBuilder();
		for (String attachment : message.getAttachments()) {
			if (attachmentBuilder.length() + attachment.length() >= MessageEmbed.VALUE_MAX_LENGTH - 1) {
				eb.addField("Attachments", attachmentBuilder.toString(),false);
				attachmentBuilder.setLength(0);
				
			} else {
				attachmentBuilder.append('\n');
			}
			attachmentBuilder.append(attachment);
		}
		eb.addField("Attachments", attachmentBuilder.toString(), false);
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
			""", UserUtils.getUserTag(author), message.getMessageId(), formatter.format(instant), message.getMessageContent());
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
			""", UserUtils.getUserTag(author), before.getMessageId(), formatter.format(instant), before.getMessageContent(), after.getContentRaw());
		return new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
	}
}
