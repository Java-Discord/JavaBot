package net.javadiscord.javabot.systems.moderation.timeout;


import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;

/**
 * Subcommand that allows staff-members to add timeouts to a single users.
 */
public class AddTimeoutSubcommand extends TimeoutSubcommand {

	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param warnRepository DAO for interacting with the set of {@link Warn} objects.
	 */
	public AddTimeoutSubcommand(NotificationService notificationService, BotConfig botConfig, ExecutorService asyncPool, WarnRepository warnRepository) {
		setSubcommandData(new SubcommandData("add", "Adds a timeout to the specified server member.")
				.addOptions(
						new OptionData(OptionType.USER, "member", "The member that should be timed out.", true),
						new OptionData(OptionType.INTEGER, "duration-amount", "How long the timeout should last.", true),
						new OptionData(OptionType.STRING, "duration-unit", "What java.lang.TimeUnit the duration should use.", true)
								.addChoice("Seconds", "SECONDS")
								.addChoice("Minutes", "MINUTES")
								.addChoice("Hours", "HOURS")
								.addChoice("Days", "DAYS")
								.addChoice("Weeks", "WEEKS"),
						new OptionData(OptionType.STRING, "reason", "The reason for adding this timeout.", true),
						new OptionData(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the timeout is issued.", false)
				)
		);
		this.notificationService=notificationService;
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
	}

	@Override
	protected ReplyCallbackAction handleTimeoutCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member member) {
		OptionMapping durationAmountOption = event.getOption("duration-amount");
		OptionMapping durationTimeUnitOption = event.getOption("duration-unit");
		OptionMapping reasonOption = event.getOption("reason");
		if (durationAmountOption == null || durationTimeUnitOption == null || reasonOption == null) {
			return Responses.replyMissingArguments(event);
		}
		// check max duration length
		Duration duration = Duration.of(durationAmountOption.getAsLong(), ChronoUnit.valueOf(durationTimeUnitOption.getAsString()));
		if (duration.toSeconds() > (Member.MAX_TIME_OUT_LENGTH * 24 * 60 * 60)) {
			return Responses.error(event, "You cannot add a timeout longer than %s days.", Member.MAX_TIME_OUT_LENGTH);
		}
		if (!event.getChannelType().isMessage()) {
			return Responses.error(event, "This command can only be performed in a server message channel.");
		}
		MessageChannel channel = event.getMessageChannel();
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		if (member.isTimedOut()) {
			return Responses.error(event, "Could not timeout %s; they're already timed out.", member.getAsMention());
		}
		ModerationService service = new ModerationService(notificationService, botConfig, event.getInteraction(), warnRepository, asyncPool);
		service.timeout(member, reasonOption.getAsString(), event.getMember(), duration, channel, quiet);
		return Responses.success(event, "User Timed Out", "%s has been timed out.", member.getAsMention());
	}
}