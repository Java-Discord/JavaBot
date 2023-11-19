package net.discordjug.javabot.data.h2db.message_cache;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.MessageCacheConfig;
import net.discordjug.javabot.data.h2db.message_cache.model.CachedMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Listener class that listens for incoming, updated or deleted messages.
 */
@RequiredArgsConstructor
public class MessageCacheListener extends ListenerAdapter {
	private final MessageCache messageCache;
	private final BotConfig botConfig;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		messageCache.cache(event.getMessage());
	}

	@Override
	public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
		if (this.ignoreMessageCache(event.getMessage())) return;
		List<CachedMessage> cache = messageCache.cache;
		Optional<CachedMessage> optional = cache.stream().filter(m -> m.getMessageId() == event.getMessageIdLong()).findFirst();
		CachedMessage before;
		if (optional.isPresent()) {
			before = optional.get();
			cache.set(cache.indexOf(before), CachedMessage.of(event.getMessage()));
		} else {
			before = new CachedMessage();
			before.setMessageId(event.getMessageIdLong());
			before.setMessageContent("[unknown content]");
			messageCache.cache(event.getMessage());
		}
		messageCache.sendUpdatedMessageToLog(event.getMessage(), before);
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		Optional<CachedMessage> optional = messageCache.cache.stream().filter(m -> m.getMessageId() == event.getMessageIdLong()).findFirst();
		optional.ifPresent(message -> {
			messageCache.sendDeletedMessageToLog(event.getGuild(), event.getChannel(), message);
			messageCache.cache.remove(message);
		});
	}


	/**
	 * Checks whether the given message should be ignored by the cache.
	 * <p>
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
		if (!message.isFromGuild()) return true;
		MessageCacheConfig config = botConfig.get(message.getGuild()).getMessageCacheConfig();
		return message.getAuthor().isBot() || message.getAuthor().isSystem() ||
				config.getExcludedUsers().contains(message.getAuthor().getIdLong()) ||
				config.getExcludedChannels().contains(message.getChannel().getIdLong());
	}

}
