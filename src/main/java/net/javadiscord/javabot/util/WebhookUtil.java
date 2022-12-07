package net.javadiscord.javabot.util;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import club.minnced.discord.webhook.send.component.LayoutComponent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Contains utility methods for dealing with Discord Webhooks.
 */
public class WebhookUtil {
	private WebhookUtil() {
	}

	/**
	 * Makes sure that a writable webhook exists in a specific channel. if no
	 * suitable webhook is found, one is created.
	 *
	 * @param channel  the {@link StandardGuildMessageChannel} the webhook should exist in
	 * @param callback an action that is executed once a webhook is
	 *                 found/created
	 */
	public static void ensureWebhookExists(@NotNull IWebhookContainer channel, @NotNull Consumer<? super Webhook> callback) {
		ensureWebhookExists(channel, callback, ExceptionLogger::capture);
	}

	/**
	 * Makes sure that a writable webhook exists in a specific channel. if no
	 * suitable webhook is found, one is created.
	 *
	 * @param channel         the {@link StandardGuildMessageChannel} the webhook should exist in
	 * @param callback        an action that is executed once a webhook is
	 *                        found/created
	 * @param failureCallback an action that is executed if the webhook
	 *                        lookup/creation failed
	 */
	public static void ensureWebhookExists(@NotNull IWebhookContainer channel, @NotNull Consumer<? super Webhook> callback, @NotNull Consumer<? super Throwable> failureCallback) {

		channel.retrieveWebhooks().queue(webhooks -> {
			Optional<Webhook> hook = webhooks.stream()
					.filter(webhook -> webhook.getChannel().getIdLong() == channel.getIdLong())
					.filter(wh -> wh.getToken() != null).findAny();
			if (hook.isPresent()) {
				callback.accept(hook.get());
			} else {
				channel.createWebhook("JavaBot-webhook").queue(callback, failureCallback);
			}
		}, failureCallback);
	}

	/**
	 * Resends a specific message using a webhook with a custom content.
	 *
	 * @param webhook           the webhook used for sending the message
	 * @param originalMessage   the message to copy
	 * @param newMessageContent the new (custom) content
	 * @param threadId          the thread to send the message in or {@code 0} if the
	 *                          message should be sent directly
	 * @param components        An optional array of {@link LayoutComponent}s.
	 * @return a {@link CompletableFuture} representing the action of sending
	 * the message
	 */
	public static CompletableFuture<ReadonlyMessage> mirrorMessageToWebhook(@NotNull Webhook webhook, @NotNull Message originalMessage, String newMessageContent, long threadId, LayoutComponent @NotNull ... components) {
		JDAWebhookClient client = new WebhookClientBuilder(webhook.getIdLong(), webhook.getToken())
				.setThreadId(threadId).buildJDA();
		WebhookMessageBuilder message = new WebhookMessageBuilder().setContent(newMessageContent)
				.setAllowedMentions(AllowedMentions.none())
				.setAvatarUrl(originalMessage.getMember().getEffectiveAvatarUrl())
				.setUsername(originalMessage.getMember().getEffectiveName());
		if (components.length > 0) {
			message.addComponents(components);
		}
		List<Attachment> attachments = originalMessage.getAttachments();
		@SuppressWarnings("unchecked")
		CompletableFuture<?>[] futures = new CompletableFuture<?>[attachments.size()];
		for (int i = 0; i < attachments.size(); i++) {
			Attachment attachment = attachments.get(i);
			futures[i] = attachment.getProxy().download().thenAccept(
					is -> message.addFile((attachment.isSpoiler() ? "SPOILER_" : "") + attachment.getFileName(), is));
		}
		return CompletableFuture.allOf(futures).thenCompose(unused -> client.send(message.build()))
				.whenComplete((result, err) -> client.close());
	}
}
