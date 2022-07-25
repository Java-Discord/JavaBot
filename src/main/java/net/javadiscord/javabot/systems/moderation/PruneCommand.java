package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.ModerationConfig;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * <h3>This class represents the /prune export-schema command.</h3>
 * This command will systematically ban users from the server if they match
 * certain criteria.
 */
public class PruneCommand extends ModerateCommand {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public PruneCommand() {
		setModerationSlashCommandData(Commands.slash("prune", "Removes members from the server.")
				.addOption(OptionType.STRING, "pattern", "A regular expression pattern to use, to remove members whose contains a match with the pattern.", false)
				.addOption(OptionType.STRING, "before", "Remove only users before the given timestamp. Format is yyyy-MM-dd HH:mm:ss, in UTC.", false)
				.addOption(OptionType.STRING, "after", "Remove only users after the given timestamp. Format is yyyy-MM-dd HH:mm:ss, in UTC.", false)
				.addOption(OptionType.STRING, "reason", "The reason for issuing the prune command. This will be provided as the reason for each ban.", false)
				.addOption(OptionType.INTEGER, "delete-days-of-history", "The number of days of the banned users' chat history to remove, between 0 and 7. Defaults to 0.", false)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator) {
		ModerationConfig config = Bot.config.get(event.getGuild()).getModerationConfig();

		OptionMapping patternOption = event.getOption("pattern");
		OptionMapping beforeOption = event.getOption("before");
		OptionMapping afterOption = event.getOption("after");
		OptionMapping reasonOption = event.getOption("reason");
		OptionMapping delDaysOption = event.getOption("delete-days-of-history");

		final Pattern pattern = patternOption == null ? null : Pattern.compile(patternOption.getAsString());
		final OffsetDateTime before = beforeOption == null ? null : LocalDateTime.parse(beforeOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
		final OffsetDateTime after = afterOption == null ? null : LocalDateTime.parse(afterOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
		final int delDays = delDaysOption == null ? 0 : (int) delDaysOption.getAsLong();
		final String reason = reasonOption == null ? null : reasonOption.getAsString();

		if (pattern == null && before == null && after == null) {
			return Responses.warning(event, "At least one filter parameter must be given; cannot remove every user from the server.");
		}
		if (delDays < 0 || delDays > 7) {
			return Responses.warning(event, "The number of days of history to delete must not be less than 0, or greater than 7.");
		}
		if (reason != null && reason.length() > 512) {
			return Responses.warning(event, "The reason for the prune cannot be more than 512 characters.");
		}

		event.getGuild().loadMembers().onSuccess(members -> {
			members.forEach(member -> {
				boolean shouldRemove = (pattern == null || pattern.matcher(member.getUser().getName()).find()) &&
						(before == null || member.getTimeJoined().isBefore(before)) &&
						(after == null || member.getTimeJoined().isAfter(after));
				if (shouldRemove) {
					config.getLogChannel().sendMessage("Removing " + member.getUser().getAsTag() + " as part of prune.").queue();
					member.ban(delDays, reason).queue();
				}
			});
		});
		return Responses.success(event, "Prune Started", "The prune action has started. Please check the log channel for information on the status of the prune.");

	}
}
