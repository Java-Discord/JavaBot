package net.javadiscord.javabot.listener;

import club.minnced.discord.webhook.send.component.ActionRow;
import club.minnced.discord.webhook.send.component.Button;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.WebhookUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for Message Links and sends the original Message if it found one.
 */
public class MessageLinkListener extends ListenerAdapter {

	private static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https://((?:canary|ptb)\\.)?discord.com/channels/[0-9]+/[0-9]+/[0-9]+");

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
		Matcher matcher = MESSAGE_URL_PATTERN.matcher(event.getMessage().getContentRaw());
		if (matcher.find()) {
			MessageChannelUnion messageChannel = event.getChannel();
			IWebhookContainer webhookChannel = getWebhookChannel(messageChannel);
			if (webhookChannel != null) {
				Optional<RestAction<Message>> optional = parseMessageUrl(matcher.group(), event.getJDA());
				optional.ifPresent(action -> action.queue(m -> {
						WebhookUtil.ensureWebhookExists(webhookChannel,
								wh -> WebhookUtil.mirrorMessageToWebhook(wh, m, m.getContentRaw(), messageChannel.getType().isThread() ? messageChannel.getIdLong() : 0, List.of(ActionRow.of(Button.link(m.getJumpUrl(), "Jump to Message"))), null));
					}, e -> ExceptionLogger.capture(e, getClass().getSimpleName())));
			}
		}
	}

	private IWebhookContainer getWebhookChannel(MessageChannelUnion channel) {
		return switch (channel.getType()) {
			case GUILD_PRIVATE_THREAD, GUILD_PUBLIC_THREAD -> getWebhookChannelFromParentChannel(channel);
			case TEXT -> channel.asTextChannel();
			default -> null;
		};
	}

	private IWebhookContainer getWebhookChannelFromParentChannel(MessageChannelUnion childChannel) {
		IThreadContainerUnion parentChannel = childChannel.asThreadChannel().getParentChannel();
		if (parentChannel.getType() == ChannelType.FORUM) {
			return parentChannel.asForumChannel();
		}
		return parentChannel.asStandardGuildMessageChannel();
	}

	/**
	 * Tries to parse a Discord Message Link to the corresponding Message object.
	 *
	 * @param url The Message Link.
	 * @param jda The {@link JDA} instance.
	 * @return An {@link Optional} containing the {@link RestAction} which retrieves the corresponding Message.
	 */
	private Optional<RestAction<Message>> parseMessageUrl(@NotNull String url, @NotNull JDA jda) {
		RestAction<Message> optional = null;
		String[] arr = url.split("/");
		String[] segments = Arrays.copyOfRange(arr, 4, arr.length);
		if (jda.getGuilds().stream().map(Guild::getId).anyMatch(s -> s.contains(segments[0]))) {
			Guild guild = jda.getGuildById(segments[0]);
			if (guild != null) {
				GuildChannel channel = guild.getGuildChannelById(segments[1]);
				if (channel instanceof MessageChannel chan) {
					optional = chan.retrieveMessageById(segments[2]);
				}
			}
		}
		return Optional.ofNullable(optional);
	}
}
