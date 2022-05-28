package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.systems.help.HelpChannelManager;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handler for the /help-ping command that allows users to occasionally ping
 * helpers.
 */
public class HelpPingCommand extends SlashCommand {
	private static final String WRONG_CHANNEL_MSG = "This command can only be used in **reserved help channels**.";
	private static final long CACHE_CLEANUP_DELAY = 60L;

	private final Map<Member, Long> lastPingTimes;

	/**
	 * Constructor that initializes and handles the cooldown map.
	 */
	public HelpPingCommand() {
		setCommandData(Commands.slash("help-ping", "Notify those with the help-ping role that your question is urgent."));
		lastPingTimes = new ConcurrentHashMap<>();
		Bot.asyncPool.scheduleWithFixedDelay(() -> {
			var membersToRemove = lastPingTimes.entrySet().stream().filter(entry -> {
				var config = Bot.config.get(entry.getKey().getGuild()).getHelp();
				long timeoutMillis = config.getHelpPingTimeoutSeconds() * 1000L;
				return entry.getValue() + timeoutMillis < System.currentTimeMillis();
			}).map(Map.Entry::getKey).toList();
			for (var member : membersToRemove) {
				lastPingTimes.remove(member);
			}
		}, CACHE_CLEANUP_DELAY, CACHE_CLEANUP_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			Responses.warning(event, WRONG_CHANNEL_MSG).queue();
			return;
		}
		HelpChannelManager channelManager = new HelpChannelManager(Bot.config.get(guild).getHelp());
		if (channelManager.isReserved(event.getTextChannel())) {
			Long lastPing = lastPingTimes.get(event.getMember());
			if (lastPing != null && lastPing + channelManager.getConfig().getHelpPingTimeoutSeconds() * 1000L > System.currentTimeMillis()) {
				Responses.warning(event, "Sorry, but you can only use this command occasionally. Please try again later.").queue();
				return;
			}
			lastPingTimes.put(event.getMember(), System.currentTimeMillis());
			Role role = channelManager.getConfig().getHelpPingRole();
			event.getChannel().sendMessage(role.getAsMention())
					.allowedMentions(EnumSet.of(Message.MentionType.ROLE))
					.setEmbeds(buildAuthorEmbed(event.getUser()))
					.queue();
			event.replyFormat("Successfully pinged " + role.getAsMention()).setEphemeral(true).queue();
		} else {
			Responses.warning(event, WRONG_CHANNEL_MSG).queue();
		}
	}

	private MessageEmbed buildAuthorEmbed(User author) {
		return new EmbedBuilder()
				.setTitle("Requested by " + author.getAsTag())
				.build();
	}
}
