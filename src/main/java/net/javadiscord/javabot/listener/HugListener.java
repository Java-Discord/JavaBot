package net.javadiscord.javabot.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.AutoMod;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.WebhookUtil;

import javax.annotation.Nonnull;

/**
 * Replaces all occurrences of 'fuck' in incoming messages with 'hug'.
 */
@Slf4j
@RequiredArgsConstructor
public class HugListener extends ListenerAdapter {
	private final AutoMod autoMod;
	private final BotConfig botConfig;

	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}
		if (autoMod.hasSuspiciousLink(event.getMessage()) || autoMod.hasAdvertisingLink(event.getMessage())) {
			return;
		}
		if (!event.getMessage().getMentions().getUsers().isEmpty()) {
			return;
		}
		if (event.isWebhookMessage()) {
			return;
		}
		if (event.getChannel().getIdLong() == botConfig.get(event.getGuild()).getModerationConfig()
				.getSuggestionChannelId()) {
			return;
		}
		TextChannel tc = null;
		if (event.isFromType(ChannelType.TEXT)) {
			tc = event.getChannel().asTextChannel();
		}
		if (event.isFromThread()) {
			StandardGuildChannel parentChannel = event.getChannel().asThreadChannel().getParentChannel().asStandardGuildChannel();
			if (parentChannel instanceof TextChannel textChannel) {
				tc = textChannel;
			}
		}
		if (tc == null) {
			return;
		}
		final TextChannel textChannel = tc;
		String content = event.getMessage().getContentRaw();
		String lowerCaseContent = content.toLowerCase();
		if (lowerCaseContent.contains("fuck")) {
			long threadId = event.isFromThread() ? event.getChannel().getIdLong() : 0;
			StringBuilder sb = new StringBuilder(content.length());
			int index = 0;
			int indexBkp = index;
			while ((index = lowerCaseContent.indexOf("fuck", index)) != -1) {
				sb.append(content.substring(indexBkp, index));
				sb.append(loadHug(content, index));
				indexBkp = index++ + 4;
				if (content.length() >= indexBkp + 3 && "ing".equals(lowerCaseContent.substring(indexBkp, indexBkp + 3))) {
					sb.append(copyCase(content, indexBkp-1, 'g'));
					sb.append(content.substring(indexBkp, indexBkp + 3));
					index+=3;
					indexBkp+=3;
				}
			}

			sb.append(content.substring(indexBkp));
			WebhookUtil.ensureWebhookExists(textChannel,
					wh -> sendWebhookMessage(wh, event.getMessage(), sb.toString(), threadId),
					e -> ExceptionLogger.capture(e, getClass().getSimpleName()));
		}
	}

	private String loadHug(String originalText, int startIndex) {
		return copyCase(originalText, startIndex, 'h') + ""
				+ copyCase(originalText, startIndex + 1, 'u') + ""
				+ copyCase(originalText, startIndex + 3, 'g');
	}

	private char copyCase(String original, int index, char newChar) {
		if (Character.isUpperCase(original.charAt(index))) {
			return Character.toUpperCase(newChar);
		} else {
			return newChar;
		}
	}

	private void sendWebhookMessage(Webhook webhook, Message originalMessage, String newMessageContent, long threadId) {
		WebhookUtil.mirrorMessageToWebhook(webhook, originalMessage, newMessageContent, threadId, null, null)
				.thenAccept(unused -> originalMessage.delete().queue()).exceptionally(e -> {
					ExceptionLogger.capture(e, getClass().getSimpleName());
					return null;
				});
	}
}
