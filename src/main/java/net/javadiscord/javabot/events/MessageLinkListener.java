package net.javadiscord.javabot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.javadiscord.javabot.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageLinkListener extends ListenerAdapter {

	private final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https://((?:canary|ptb)\\.)?discord.com/channels/[0-9]+/[0-9]+/[0-9]+");

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;
		Matcher matcher = MESSAGE_URL_PATTERN.matcher(event.getMessage().getContentRaw());
		if (matcher.find()) {
			var optional = parseMessageUrl(matcher.group(), event.getJDA());
			optional.ifPresent(action -> action.queue(m -> event.getMessage().replyEmbeds(buildUrlEmbed(m)).queue(), e -> {}));
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

	private Optional<RestAction<Message>> parseMessageUrl(String url, JDA jda) {
		RestAction<Message> optional = null;
		try {
			var arr = url.split("/");
			String[] segments = Arrays.copyOfRange(arr, 4, arr.length);
			if (jda.getGuilds().stream().map(Guild::getId).anyMatch(s -> s.contains(segments[0]))) {
				var guild = jda.getGuildById(segments[0]);
				if (guild != null && guild.getChannels().stream().map(GuildChannel::getId).anyMatch(s -> s.contains(segments[1]))) {
					var channel = guild.getTextChannelById(segments[1]);
					if (channel != null) {
						optional = channel.retrieveMessageById(segments[2]);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.ofNullable(optional);
	}
}
