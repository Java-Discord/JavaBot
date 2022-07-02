package net.javadiscord.javabot.listener;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.WebhookUtil;

/**
 * Replaces all occurences of 'fuck' in incoming messages with 'hug'.
 */
@Slf4j
public class HugListener extends ListenerAdapter {
	@Override
	public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
		if (!event.isFromGuild()) {
			return;
		}
		if (Bot.autoMod.hasSuspiciousLink(event.getMessage()) || Bot.autoMod.hasAdvertisingLink(event.getMessage())) {
			return;
		}
		if (!event.getMessage().getMentions().getUsers().isEmpty()) {
			return;
		}
		if (event.isWebhookMessage()) {
			return;
		}
		TextChannel tc = null;
		if (event.isFromType(ChannelType.TEXT)) {
			tc = event.getTextChannel();
		}
		if (event.isFromThread()) {
			GuildMessageChannel parentChannel = event.getThreadChannel().getParentMessageChannel();
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
			long threadId = event.isFromThread() ? event.getThreadChannel().getIdLong() : 0;
			StringBuilder sb = new StringBuilder(content.length());
			int index = 0;
			int indexBkp = index;
			while ((index = lowerCaseContent.indexOf("fuck", index)) != -1) {
				sb.append(content.substring(indexBkp, index));
				sb.append("hug");
				indexBkp = index++ + 4;
			}

			sb.append(content.substring(indexBkp, content.length()));
			WebhookUtil.ensureWebhookExists(textChannel,
					wh -> sendWebhookMessage(wh, event.getMessage(), sb.toString(), threadId),
					e -> log.error("Webhook lookup/creation failed", e));
		}
	}

	private void sendWebhookMessage(Webhook webhook, Message originalMessage, String newMessageContent, long threadId) {
		WebhookUtil.mirrorMessageToWebhook(webhook, originalMessage, newMessageContent, threadId)
				.thenAccept(unused -> originalMessage.delete().queue()).exceptionally(e -> {
					log.error("replacing the content 'fuck' with 'hug' in an incoming message failed", e);
					return null;
				});
	}
}
