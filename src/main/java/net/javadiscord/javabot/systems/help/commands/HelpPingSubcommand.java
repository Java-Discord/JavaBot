package net.javadiscord.javabot.systems.help.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handler for the /help ping sub-command that allows users to occasionally ping
 * helpers.
 */
public class HelpPingSubcommand extends SlashCommand.Subcommand {
	private static final String WRONG_CHANNEL_MSG = "This command can only be used in **help forum posts.**.";
	private static final long CACHE_CLEANUP_DELAY = 60L;

	private final Map<Long, Pair<Long, Guild>> lastPingTimes;
	private final BotConfig botConfig;

	/**
	 * Constructor that initializes and handles the cooldown map.
	 * @param asyncPool The thread pool for asynchronous operations
	 * @param botConfig The main configuration of the bot
	 */
	public HelpPingSubcommand(BotConfig botConfig, ScheduledExecutorService asyncPool) {
		setCommandData(new SubcommandData("ping", "Notify those with the help-ping role that your question is urgent."));
		lastPingTimes = new ConcurrentHashMap<>();
		this.botConfig = botConfig;
		asyncPool.scheduleWithFixedDelay(this::cleanTimeoutCache, CACHE_CLEANUP_DELAY, CACHE_CLEANUP_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			Responses.warning(event, WRONG_CHANNEL_MSG).queue();
			return;
		}
		GuildConfig config = botConfig.get(guild);
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (post.getParentChannel().getType() != ChannelType.FORUM ||
				post.getParentChannel().getIdLong() != config.getHelpForumConfig().getHelpForumChannelId()
		) {
			Responses.error(event, WRONG_CHANNEL_MSG).queue();
			return;
		}
		Member member = event.getMember();
		if (member == null) {
			Responses.warning(event, "No member information was available for this event.").queue();
			return;
		}
		if (isHelpPingForbiddenForMember(post.getOwnerIdLong(), member, config)) {
			Responses.warning(event, "Sorry, but only the person who reserved this channel, or staff and helpers, may use this command.").queue();
			return;
		}
		if (isHelpPingTimeoutElapsed(member.getIdLong(), config)) {
			lastPingTimes.put(event.getMember().getIdLong(), new Pair<>(System.currentTimeMillis(), config.getGuild()));
			Role role = config.getHelpConfig().getHelpPingRole();
			event.getChannel().sendMessage(role.getAsMention())
					.setAllowedMentions(EnumSet.of(Message.MentionType.ROLE))
					.setEmbeds(buildAuthorEmbed(event.getUser()))
					.queue();
			event.replyFormat("Successfully pinged " + role.getAsMention()).setEphemeral(true).queue();
		} else {
			Responses.warning(event, "Sorry, but you can only use this command occasionally. Please try again later.").queue();
		}
	}

	private @NotNull MessageEmbed buildAuthorEmbed(@NotNull User author) {
		return new EmbedBuilder()
				.setTitle("Requested by " + author.getAsTag())
				.build();
	}

	/**
	 * Determines if a user is forbidden from sending a help-ping command due
	 * to their status in the server.
	 *
	 * @param postOwnerId The posts' owner id.
	 * @param member      The member.
	 * @param config      The guild config.
	 * @return True if the user is forbidden from sending the command.
	 */
	private boolean isHelpPingForbiddenForMember(long postOwnerId, @NotNull Member member, @NotNull GuildConfig config) {
		Set<Role> allowedRoles = Set.of(config.getModerationConfig().getStaffRole(), config.getHelpConfig().getHelperRole());
		return !(
				postOwnerId == member.getUser().getIdLong() ||
						member.getRoles().stream().anyMatch(allowedRoles::contains) ||
						member.isOwner()
		);
	}

	/**
	 * Determines if the user's timeout has elapsed (or doesn't exist), which
	 * implies that it's fine for the user to send the command.
	 *
	 * @param memberId The members' id.
	 * @param config   The guild config.
	 * @return True if the user's timeout has elapsed or doesn't exist, or
	 * false if the user should NOT send the command because of their timeout.
	 */
	private boolean isHelpPingTimeoutElapsed(long memberId, GuildConfig config) {
		Pair<Long, Guild> lastPing = lastPingTimes.get(memberId);
		return lastPing == null ||
				lastPing.first() + config.getHelpConfig().getHelpPingTimeoutSeconds() * 1000L < System.currentTimeMillis();
	}

	/**
	 * Method that cleans out any entries from the list of last ping times if
	 * their timeout is no longer valid.
	 */
	private void cleanTimeoutCache() {
		// Find the list of members whose last ping time was old enough that they should be removed from the cache.
		List<Long> memberIdsToRemove = lastPingTimes.entrySet().stream().filter(entry -> {
			HelpConfig config = botConfig.get(entry.getValue().second()).getHelpConfig();
			long timeoutMillis = config.getHelpPingTimeoutSeconds() * 1000L;
			return entry.getValue().first() + timeoutMillis < System.currentTimeMillis();
		}).map(Map.Entry::getKey).toList();
		// Remove each member from the map.
		for (Long memberId : memberIdsToRemove) {
			lastPingTimes.remove(memberId);
		}
	}
}
