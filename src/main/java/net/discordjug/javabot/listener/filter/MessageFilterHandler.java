package net.discordjug.javabot.listener.filter;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.AutoMod;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.WebhookUtil;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for calling {@link MessageFilter}s on incoming messages and optionally replacing the message.
 *
 * When a message is received, registered {@link MessageFilter filters} are executed sequentially to process the message.
 * These filters are able to act on the message and modify message contents using the {@link MessageContent} record.
 * Modifications to the message are performed after all filters are executed by deleting the original message and re-sending a modified message.
 *
 * Message filters are Spring components implementing the {@link MessageFilter} interface.
 */
@RequiredArgsConstructor
public class MessageFilterHandler extends ListenerAdapter {

	private final List<MessageFilter> filters;
	private final AutoMod autoMod;
	private final BotConfig botConfig;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!shouldRunFilters(event)) {
			return;
		}

		MessageContent content = new MessageContent(
				event,
				new StringBuilder(event.getMessage().getContentRaw()),
				new ArrayList<>(event.getMessage().getAttachments()),
				new ArrayList<>(event.getMessage().getEmbeds())
		);

		boolean handled = false;

		for (MessageFilter filter : filters) {
			MessageModificationStatus status = filter.processMessage(content);
			if (status == MessageModificationStatus.MODIFIED) {
				handled = true;
			} else if (status == MessageModificationStatus.STOP_PROCESSING) {
				return;
			}
		}

		if (handled) {
			IWebhookContainer webhookContainer = null;
			long threadId = 0;
			if (event.isFromType(ChannelType.TEXT)) {
				webhookContainer = event.getChannel().asTextChannel();
			}
			if (event.isFromThread()) {
				StandardGuildChannel parentChannel = event.getChannel()
						.asThreadChannel()
						.getParentChannel()
						.asStandardGuildChannel();
				threadId = event.getChannel().getIdLong();
				webhookContainer = (IWebhookContainer) parentChannel;
			}
			if (webhookContainer == null) {
				return;
			}
			replaceMessage(webhookContainer, threadId, content);
		}
	}

	private boolean shouldRunFilters(@NotNull MessageReceivedEvent event) {
		if (event.isWebhookMessage()) {
			return false;
		}
		if (!event.isFromGuild()) {
			return false;
		}
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
			return false;
		}
		if (autoMod.hasSuspiciousLink(event.getMessage()) ||
				autoMod.hasAdvertisingLink(event.getMessage())) {
			return false;
		}
		if (event.getChannel().getIdLong() == botConfig.get(event.getGuild())
				.getModerationConfig()
				.getSuggestionChannelId()) {
			return false;
		}
		return true;
	}

	private void replaceMessage(IWebhookContainer webhookContainer, long threadId, MessageContent content) {
		WebhookUtil.ensureWebhookExists(
				webhookContainer,
				wh -> WebhookUtil.replaceMemberMessage(
						wh,
						content.event().getMessage(),
						content.messageText().toString(),
						threadId,
						content.attachments(),
						content.embeds()
				),
				e -> ExceptionLogger.capture(
						e,
						this.getClass().getSimpleName()
				)
		);
	}
}
