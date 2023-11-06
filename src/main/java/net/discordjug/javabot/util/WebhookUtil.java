package net.discordjug.javabot.util;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import club.minnced.discord.webhook.send.component.LayoutComponent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

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
	 * @param channel  the {@link IWebhookContainer} the webhook should exist in
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
	 * @param channel         the {@link IWebhookContainer} the webhook should exist in
	 * @param callback        an action that is executed once a webhook is
	 *                        found/created
	 * @param failureCallback an action that is executed if the webhook
	 *                        lookup/creation failed
	 */
	public static void ensureWebhookExists(@NotNull IWebhookContainer channel, @NotNull Consumer<? super Webhook> callback, @NotNull Consumer<? super Throwable> failureCallback) {

		Consumer<? super Webhook> safeCallback = wh -> {
			try {
				callback.accept(wh);
				//CHECKSTYLE:OFF: IllegalCatch - This should make sure it is properly logged if anything bad happens
			} catch (Exception e) {
				//CHECKSTYLE:ON: IllegalCatch
				failureCallback.accept(e);
			}
		};
		channel.retrieveWebhooks().queue(webhooks -> {
			Optional<Webhook> hook = webhooks.stream()
					.filter(webhook -> webhook.getChannel().getIdLong() == channel.getIdLong())
					.filter(wh -> wh.getOwner() != null)
					.filter(wh -> wh.getOwner().getIdLong() == channel.getJDA().getSelfUser().getIdLong())
					.filter(wh -> wh.getToken() != null)
					.findAny();
			if (hook.isPresent()) {
				safeCallback.accept(hook.get());
			} else {
				channel.createWebhook("JavaBot-webhook").queue(safeCallback, failureCallback);
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
	 * @param components        A nullable list of {@link LayoutComponent}s.
	 * @param embeds            A nullable list of {@link MessageEmbed}s.
	 * @return a {@link CompletableFuture} representing the action of sending
	 * the message
	 */
	public static CompletableFuture<ReadonlyMessage> mirrorMessageToWebhook(@NotNull Webhook webhook, @NotNull Message originalMessage, String newMessageContent, long threadId, @Nullable List<LayoutComponent> components, @Nullable List<MessageEmbed> embeds) {
		JDAWebhookClient client = new WebhookClientBuilder(webhook.getIdLong(), webhook.getToken()).setThreadId(threadId)
				.buildJDA();
		WebhookMessageBuilder message = new WebhookMessageBuilder().setContent(newMessageContent)
				.setAllowedMentions(AllowedMentions.none())
				.setAvatarUrl(transformOrNull(originalMessage.getMember(), Member::getEffectiveAvatarUrl))
				.setUsername(transformOrNull(originalMessage.getMember(), Member::getEffectiveName));
		if (components != null && !components.isEmpty()) {
			message.addComponents(components);
		}

		if (embeds == null || embeds.isEmpty()) {
			embeds = originalMessage.getEmbeds();
		}
		message.addEmbeds(embeds.stream()
				.map(e -> WebhookEmbedBuilder.fromJDA(e).build())
				.toList());
		List<Attachment> attachments = originalMessage.getAttachments();
		@SuppressWarnings("unchecked")
		CompletableFuture<?>[] futures = new CompletableFuture<?>[attachments.size()];
		for (int i = 0; i < attachments.size(); i++) {
			Attachment attachment = attachments.get(i);
			futures[i] = attachment.getProxy()
					.download()
					.thenAccept(is -> message.addFile((attachment.isSpoiler() ? "SPOILER_" : "") + attachment.getFileName(), is));
		}
		return CompletableFuture.allOf(futures)
				.thenCompose(unused -> sendMessage(client, message))
				.whenComplete((result, err) -> {
					client.close();
					if (err != null) {
						ExceptionLogger.capture(err, WebhookUtil.class.getSimpleName());
					}
				});
	}

	private static <T, R> R transformOrNull(T toTransform, Function<T, R> transformer) {
		return toTransform == null ? null : transformer.apply(toTransform);
	}

	private static @NotNull CompletableFuture<ReadonlyMessage> sendMessage(JDAWebhookClient client, WebhookMessageBuilder message) {
		if (message.isEmpty()) {
			message.setContent("<empty message>");
		}
		return client.send(message.build());
	}

	/**
	 * Method for replacing a user's guild message through a webhook.
	 *
	 * @param webhook           a reference to a webhook
	 * @param originalMessage   a reference to the {@link Message} object that should be replaced
	 * @param newMessageContent a String containing the new message's content
	 * @param threadId          id of the thread in which the message should be replaced
	 * @param embeds            optional additional embeds to be added
	 */
	public static void replaceMemberMessage(Webhook webhook, Message originalMessage, String newMessageContent, long threadId, MessageEmbed... embeds) {
		WebhookUtil.mirrorMessageToWebhook(webhook, originalMessage, newMessageContent, threadId, null, List.of(embeds))
				.thenAccept(unused -> originalMessage.delete().queue())
				.exceptionally(e -> {
					ExceptionLogger.capture(e, WebhookUtil.class.getSimpleName());
					return null;
				});
	}
}
