package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.InteractionUtils;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens for Message Links and sends the original Message if it found one.
 */
public class MessageLinkListener extends ListenerAdapter {

	private final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https://((?:canary|ptb)\\.)?discord.com/channels/[0-9]+/[0-9]+/[0-9]+");

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
		Matcher matcher = MESSAGE_URL_PATTERN.matcher(event.getMessage().getContentRaw());
		if (matcher.find()) {
			Optional<RestAction<Message>> optional = this.parseMessageUrl(matcher.group(), event.getJDA());
			optional.ifPresent(action -> action.queue(
					m -> event.getMessage().replyEmbeds(this.buildUrlEmbed(m))
							.setActionRow(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"), Button.link(m.getJumpUrl(), "View Original"))
							.queue(),
					e -> {}
			));
		}
	}

	private MessageEmbed buildUrlEmbed(Message m) {
		User author = m.getAuthor();
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), m.getJumpUrl(), author.getEffectiveAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(m.getContentRaw())
				.setTimestamp(m.getTimeCreated())
				.setFooter("#" + m.getChannel().getName())
				.build();
	}

	/**
	 * Tries to parse a Discord Message Link to the corresponding Message object.
	 *
	 * @param url The Message Link.
	 * @param jda The {@link JDA} instance.
	 * @return An {@link Optional} containing the {@link RestAction} which retrieves the corresponding Message.
	 */
	private Optional<RestAction<Message>> parseMessageUrl(String url, JDA jda) {
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
