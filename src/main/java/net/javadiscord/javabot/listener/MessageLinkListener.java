package net.javadiscord.javabot.listener;

import club.minnced.discord.webhook.send.component.ActionRow;
import club.minnced.discord.webhook.send.component.Button;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.WebhookUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
			Optional<RestAction<Message>> optional = parseMessageUrl(matcher.group(), event.getJDA());
			optional.ifPresent(action -> action.queue(
					m -> WebhookUtil.ensureWebhookExists(event.getChannel().asTextChannel(),
							wh -> WebhookUtil.mirrorMessageToWebhook(wh, m, m.getContentRaw(), 0, ActionRow.of(Button.link(m.getJumpUrl(), "Jump to Message")))
					), e -> ExceptionLogger.capture(e, getClass().getSimpleName())
			));
		}
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
