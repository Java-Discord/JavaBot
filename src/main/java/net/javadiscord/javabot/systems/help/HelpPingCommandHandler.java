package net.javadiscord.javabot.systems.help;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handler for the /help-ping command that allows users to occasionally ping
 * helpers.
 */
public class HelpPingCommandHandler implements SlashCommand {
	private static final String WRONG_CHANNEL_MSG = "This command can only be used in **reserved help channels**.";
	private static final long CACHE_CLEANUP_DELAY = 60L;

	private final Map<Member, Long> lastPingTimes;

	/**
	 * Constructor that initializes and handles the cooldown map.
	 */
	public HelpPingCommandHandler() {
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
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		Guild guild = event.getGuild();
		if (guild == null) return Responses.warning(event, WRONG_CHANNEL_MSG);
		var channelManager = new HelpChannelManager(Bot.config.get(guild).getHelp());
		if (channelManager.isReserved(event.getTextChannel())) {
			Long lastPing = lastPingTimes.get(event.getMember());
			if (lastPing != null && lastPing + channelManager.getConfig().getHelpPingTimeoutSeconds() * 1000L > System.currentTimeMillis()) {
				return Responses.warning(event, "Sorry, but you can only use this command occasionally. Please try again later.");
			}
			lastPingTimes.put(event.getMember(), System.currentTimeMillis());
			var role = channelManager.getConfig().getHelpPingRole();
			event.getChannel().sendMessage(role.getAsMention()).queue();
			return event.replyFormat("Done!").setEphemeral(true);
		} else {
			return Responses.warning(event, WRONG_CHANNEL_MSG);
		}
	}
}
