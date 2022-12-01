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
import net.javadiscord.javabot.util.WebhookUtil;
import org.jetbrains.annotations.NotNull;

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
		// cannot be higher; words like "fun" will be mistaken
		if (damerauLevenshteinVsFuck(lowerCaseContent) <= 1) {
			long threadId = event.isFromThread() ? event.getChannel().getIdLong() : 0;
			StringBuilder sb = new StringBuilder(content.length());
			int index = 0;
			int indexBkp = index;
			while ((index = lowerCaseContent.indexOf("fuck", index)) != -1) {
				sb.append(content, indexBkp, index);
				sb.append(loadHug(content, index));
				indexBkp = index++ + 4;
				if (content.length() >= indexBkp + 3 && "ing".equals(lowerCaseContent.substring(indexBkp, indexBkp + 3))) {
					sb.append(copyCase(content, indexBkp-1, 'g'));
					sb.append(content, indexBkp, indexBkp + 3);
					index+=3;
					indexBkp+=3;
				}
			}

			sb.append(content.substring(indexBkp));
			WebhookUtil.ensureWebhookExists(textChannel,
					wh -> sendWebhookMessage(wh, event.getMessage(), sb.toString(), threadId),
					e -> log.error("Webhook lookup/creation failed", e));
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
		WebhookUtil.mirrorMessageToWebhook(webhook, originalMessage, newMessageContent, threadId)
				.thenAccept(unused -> originalMessage.delete().queue()).exceptionally(e -> {
					log.error("replacing the content 'fuck' with 'hug' in an incoming message failed", e);
					return null;
				});
	}

	/**
	 * Calculates the true Damerau-Levenshtein string distance (with adjacent transpositions) of a {@link String}
	 * against the string {@code "fuck"}.
	 * @param string The string to compare against.
	 * @return the distance of the given string.
	 */
	private int damerauLevenshteinVsFuck(@NotNull String string) {
		int sourceLength = string.length();
		int targetLength = "fuck".length();
		if (sourceLength == 0) return targetLength;
		int[][] dist = new int[sourceLength + 1][targetLength + 1];
		for (int i = 0; i < sourceLength + 1; i++) {
			dist[i][0] = i;
		}
		for (int j = 0; j < targetLength + 1; j++) {
			dist[0][j] = j;
		}
		for (int i = 1; i < sourceLength + 1; i++) {
			for (int j = 1; j < targetLength + 1; j++) {
				int cost = string.charAt(i - 1) == "fuck".charAt(j - 1) ? 0 : 1;
				dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost);
				if (i > 1 && j > 1 && string.charAt(i - 1) == "fuck".charAt(j - 2) && string.charAt(i - 2) == "fuck".charAt(j - 1)) {
					dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
				}
			}
		}
		return dist[sourceLength][targetLength];
	}

}
