package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.systems.help.HelpChannelManager;
import net.javadiscord.javabot.systems.help.model.ChannelReservation;
import net.javadiscord.javabot.util.Pair;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handler for the /help ping sub-command that allows users to occasionally ping
 * helpers.
 */
public class HelpPingSubcommand extends SlashCommand.Subcommand {
	private static final String WRONG_CHANNEL_MSG = "This command can only be used in **reserved help channels**.";
	private static final long CACHE_CLEANUP_DELAY = 60L;

	private final Map<Long, Pair<Long, Guild>> lastPingTimes;

	/**
	 * Constructor that initializes and handles the cooldown map.
	 */
	public HelpPingSubcommand() {
		setSubcommandData(new SubcommandData("ping", "Notify those with the help-ping role that your question is urgent."));
		lastPingTimes = new ConcurrentHashMap<>();
		Bot.getAsyncPool().scheduleWithFixedDelay(this::cleanTimeoutCache, CACHE_CLEANUP_DELAY, CACHE_CLEANUP_DELAY, TimeUnit.SECONDS);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) {
			Responses.warning(event, WRONG_CHANNEL_MSG).queue();
			return;
		}
		GuildConfig config = Bot.getConfig().get(guild);
		HelpChannelManager channelManager = new HelpChannelManager(config.getHelpConfig());
		if (channelManager.isReserved(event.getChannel().asTextChannel())) {
			Optional<ChannelReservation> optionalReservation = channelManager.getReservationForChannel(event.getChannel().getIdLong());
			if (optionalReservation.isEmpty()) {
				Responses.warning(event, "Could not fetch the channel reservation.").queue();
				return;
			}
			ChannelReservation reservation = optionalReservation.get();
			Member member = event.getMember();
			if (member == null) {
				Responses.warning(event, "No member information was available for this event.").queue();
				return;
			}
			if (isHelpPingForbiddenForMember(reservation, member, config)) {
				Responses.warning(event, "Sorry, but only the person who reserved this channel, or staff and helpers, may use this command.").queue();
				return;
			}
			if (isHelpPingTimeoutElapsed(member.getIdLong(), config)) {
				lastPingTimes.put(event.getMember().getIdLong(), new Pair<>(System.currentTimeMillis(), guild));
				Role role = channelManager.getConfig().getHelpPingRole();
				event.getChannel().sendMessage(role.getAsMention())
						.setAllowedMentions(EnumSet.of(Message.MentionType.ROLE))
						.setEmbeds(buildAuthorEmbed(event.getUser()))
						.queue();
				event.replyFormat("Successfully pinged " + role.getAsMention()).setEphemeral(true).queue();
			} else {
				Responses.warning(event, "Sorry, but you can only use this command occasionally. Please try again later.").queue();
			}
		} else {
			Responses.warning(event, WRONG_CHANNEL_MSG).queue();
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
	 * @param reservation The channel reservation for the channel they're
	 *                    trying to send the command in.
	 * @param member      The member.
	 * @param config      The guild config.
	 * @return True if the user is forbidden from sending the command.
	 */
	private boolean isHelpPingForbiddenForMember(@NotNull ChannelReservation reservation, @NotNull Member member, @NotNull GuildConfig config) {
		Set<Role> allowedRoles = Set.of(config.getModerationConfig().getStaffRole(), config.getHelpConfig().getHelperRole());
		return !(
				reservation.getUserId() == member.getUser().getIdLong() ||
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
			HelpConfig config = Bot.getConfig().get(entry.getValue().second()).getHelpConfig();
			long timeoutMillis = config.getHelpPingTimeoutSeconds() * 1000L;
			return entry.getValue().first() + timeoutMillis < System.currentTimeMillis();
		}).map(Map.Entry::getKey).toList();
		// Remove each member from the map.
		for (Long memberId : memberIdsToRemove) {
			lastPingTimes.remove(memberId);
		}
	}
}
