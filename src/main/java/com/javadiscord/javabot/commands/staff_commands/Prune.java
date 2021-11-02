package com.javadiscord.javabot.commands.staff_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * This command will systematically ban users from the server if they match
 * certain criteria.
 */
public class Prune implements SlashCommandHandler {
	private final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			return Responses.warning(event, "This command can only be used in a guild.");
		}

		var config = Bot.config.get(event.getGuild()).getModeration();

		OptionMapping patternOption = event.getOption("pattern");
		OptionMapping beforeOption = event.getOption("before");
		OptionMapping afterOption = event.getOption("after");
		OptionMapping reasonOption = event.getOption("reason");
		OptionMapping delDaysOption = event.getOption("delete-days-of-history");

		final var pattern = patternOption == null ? null : Pattern.compile(patternOption.getAsString());
		final var before = beforeOption == null ? null : LocalDateTime.parse(beforeOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
		final var after = afterOption == null ? null : LocalDateTime.parse(afterOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
		final var delDays = delDaysOption == null ? 0 : (int) delDaysOption.getAsLong();
		final var reason = reasonOption == null ? null : reasonOption.getAsString();

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
