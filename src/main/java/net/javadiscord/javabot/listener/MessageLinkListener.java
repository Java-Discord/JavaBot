package net.javadiscord.javabot.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.Bot;
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
			var optional = this.parseMessageUrl(matcher.group(), event.getJDA());
			optional.ifPresent(action -> action.queue(m -> event.getMessage().replyEmbeds(this.buildUrlEmbed(m)).queue(), e -> {
			}));
		}
	}

	private MessageEmbed buildUrlEmbed(Message m) {
		return new EmbedBuilder()
				.setAuthor("Jump to Original", m.getJumpUrl())
				.setColor(Bot.config.get(m.getGuild()).getSlashCommand().getDefaultColor())
				.setDescription(m.getContentRaw())
				.setTimestamp(m.getTimeCreated())
				.setFooter(String.format("%s in #%s", m.getAuthor().getAsTag(), m.getChannel().getName()), m.getAuthor().getEffectiveAvatarUrl())
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
			if (guild != null && guild.getChannels().stream().map(GuildChannel::getId).anyMatch(s -> s.contains(segments[1]))) {
				TextChannel channel = guild.getTextChannelById(segments[1]);
				if (channel != null) {
					optional = channel.retrieveMessageById(segments[2]);
				}
			}
		}
		return Optional.ofNullable(optional);
	}
}
