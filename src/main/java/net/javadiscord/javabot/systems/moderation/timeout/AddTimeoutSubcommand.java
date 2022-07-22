package net.javadiscord.javabot.systems.moderation.timeout;


import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Subcommand that allows staff-members to add timeouts to a single users.
 */
public class AddTimeoutSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public AddTimeoutSubcommand() {
		setSubcommandData(new SubcommandData("add", "Adds a Timeout to the given User.")
				.addOptions(
						new OptionData(OptionType.USER, "user", "The user that should be timed out.", true),
						new OptionData(OptionType.INTEGER, "duration-amount", "How long the Timeout should last.", true),
						new OptionData(OptionType.STRING, "duration-unit", "What Timeunit your Duration should have.", true)
								.addChoices(new Command.Choice("Seconds", "SECONDS"),
										new Command.Choice("Minutes", "MINUTES"),
										new Command.Choice("Hours", "HOURS"),
										new Command.Choice("Days", "DAYS"),
										new Command.Choice("Weeks", "WEEKS")),
						new OptionData(OptionType.STRING, "reason", "The reason for adding this Timeout.", true),
						new OptionData(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the Timeout is issued.", false)
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userOption = event.getOption("user");
		OptionMapping durationAmountOption = event.getOption("duration-amount");
		OptionMapping durationTimeUnitOption = event.getOption("duration-unit");
		OptionMapping reasonOption = event.getOption("reason");
		if (userOption == null || durationAmountOption == null || durationTimeUnitOption == null || reasonOption == null) {
			Responses.missingArguments(event).queue();
			return;
		}
		Member member = userOption.getAsMember();
		if (member == null) {
			Responses.error(event, "Cannot timeout a user who is not a member of this server").queue();
			return;
		}
		if (!Checks.hasPermission(event.getGuild(), Permission.MODERATE_MEMBERS) || !event.getGuild().getSelfMember().canInteract(member)) {
			Responses.error(event, "Insufficient Permissions").queue();
			return;
		}
		String reason = reasonOption.getAsString();
		Duration duration = Duration.of(durationAmountOption.getAsLong(), ChronoUnit.valueOf(durationTimeUnitOption.getAsString()));
		if (duration.toSeconds() > (Member.MAX_TIME_OUT_LENGTH * 24 * 60 * 60)) {
			Responses.error(event, "You cannot add a Timeout longer than %s days.", Member.MAX_TIME_OUT_LENGTH).queue();
			return;
		}
		MessageChannel channel = event.getMessageChannel();
		if (!channel.getType().isMessage()) {
			Responses.error(event, "This command can only be performed in a server message channel.").queue();
			return;
		}
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		if (member.isTimedOut()) {
			Responses.error(event, "Could not timeout %s; they are already timed out.", member.getAsMention()).queue();
			return;
		}
		ModerationService moderationService = new ModerationService(event.getInteraction());
		moderationService.timeout(member, reason, event.getMember(), duration, channel, quiet);
		Responses.success(event, "User Timed Out", "%s has been timed out.", member.getAsMention()).queue();
	}
}