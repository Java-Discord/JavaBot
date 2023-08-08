package net.javadiscord.javabot.listener;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
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
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Replaces all occurrences of 'fuck' in incoming messages with 'hug'.
 */
@RequiredArgsConstructor
public class HugListener extends ListenerAdapter {
	private static final Pattern FUCKER = Pattern.compile("(fuck)(ing|er|ed|k+)?", Pattern.CASE_INSENSITIVE);
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
		String content = event.getMessage().getContentRaw();
		if (FUCKER.matcher(content).find()) {
			long threadId = event.isFromThread() ? event.getChannel().getIdLong() : 0;
			WebhookUtil.ensureWebhookExists(tc,
					wh -> sendWebhookMessage(wh, event.getMessage(), replaceFucks(content), threadId),
					e -> ExceptionLogger.capture(e, getClass().getSimpleName()));
		}
	}

	private static String processHug(String originalText) {
		// FucK -> HuG, FuCk -> Hug
		return String.valueOf(copyCase(originalText, 0, 'h'))
			+ copyCase(originalText, 1, 'u')
			+ copyCase(originalText, 3, 'g');
	}

	private static String replaceFucks(String str) {
		return FUCKER.matcher(str).replaceAll(matchResult -> {
			String theFuck = matchResult.group(1);
			String suffix = Objects.requireNonNullElse(matchResult.group(2), "");
			String processedSuffix = switch(suffix.toLowerCase()) {
				case "er", "ed", "ing" -> copyCase(suffix, 0, 'g') + suffix; // fucking, fucker, fucked
				case "" -> ""; // just fuck
				default -> copyCase(suffix, "g".repeat(suffix.length())); // fuckkkkk...
			};
			return processHug(theFuck) + processedSuffix;
		});
	}

	private static String copyCase(String source, String toChange) {
		if (source.length() != toChange.length()) throw new IllegalArgumentException("lengths differ");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < source.length(); i++) {
			sb.append(copyCase(source, i, toChange.charAt(i)));
		}
		return sb.toString();
	}

	private static char copyCase(String original, int index, char newChar) {
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
