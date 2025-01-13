package net.discordjug.javabot.systems.moderation;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.data.config.guild.ModerationConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * <h3>This class represents the /prune command.</h3>
 * This command will systematically ban users from the server if they match
 * certain criteria.
 */
@Slf4j
public class PruneCommand extends ModerateCommand {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public PruneCommand(BotConfig botConfig) {
		super(botConfig);
		setModerationSlashCommandData(Commands.slash("prune", "Removes members from the server.")
				.addOption(OptionType.STRING, "pattern", "A regular expression pattern to use, to remove members whose contains a match with the pattern.", false)
				.addOption(OptionType.STRING, "before", "Remove only users before the given timestamp. Format is yyyy-MM-dd HH:mm:ss, in UTC.", false)
				.addOption(OptionType.STRING, "after", "Remove only users after the given timestamp. Format is yyyy-MM-dd HH:mm:ss, in UTC.", false)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator) {
		if (!Checks.hasAdminRole(botConfig, moderator)) {
			return Responses.replyAdminOnly(event, botConfig.get(event.getGuild()));
		}
		ModerationConfig config = botConfig.get(event.getGuild()).getModerationConfig();

		OptionMapping patternOption = event.getOption("pattern");
		OptionMapping beforeOption = event.getOption("before");
		OptionMapping afterOption = event.getOption("after");

		final Pattern pattern = patternOption == null ? null : Pattern.compile(patternOption.getAsString());
		final OffsetDateTime before = beforeOption == null ? null : LocalDateTime.parse(beforeOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);
		final OffsetDateTime after = afterOption == null ? null : LocalDateTime.parse(afterOption.getAsString(), TIMESTAMP_FORMATTER).atOffset(ZoneOffset.UTC);

		if (pattern == null && before == null && after == null) {
			return Responses.warning(event, "At least one filter parameter must be given; cannot remove every user from the server.");
		}
		String pruneRoleName = "prune-" + LocalDateTime.now();
		event.getGuild().createRole().setName(pruneRoleName).queue(role -> {
			event.getGuild().loadMembers().onSuccess(members -> {
				AtomicInteger count = new AtomicInteger(0);
				members.forEach(member -> {
					boolean shouldRemove = (pattern == null || pattern.matcher(member.getUser().getName()).find()) &&
							(before == null || member.getTimeJoined().isBefore(before)) &&
							(after == null || member.getTimeJoined().isAfter(after));
					if (shouldRemove) {
						if(member.getUser().isBot() || !moderator.canInteract(member) || Checks.hasStaffRole(botConfig, member)) {
							config.getLogChannel()
								.sendMessage("# WARNING\nPrune by " + moderator.getAsMention() + " would affect " + UserUtils.getUserTag(member.getUser()) + " who is a privileged user! This is likely not intentional.")
								.queue();
							return;
						}
						log.info("Marking " + UserUtils.getUserTag(member.getUser()) + " with " + pruneRoleName +" as part of prune.");
						config.getLogChannel()
							.sendMessage("Marking " + member.getAsMention() + " (" + UserUtils.getUserTag(member.getUser()) + ") with " + role.getAsMention() +" as part of prune.")
							.setAllowedMentions(List.of())
							.queue();
						event.getGuild().addRoleToMember(member, role).queue();
						count.incrementAndGet();
					}
				});
				int finalCount = count.get();
				config.getLogChannel().sendMessage("Prune by "+ moderator.getAsMention() +" complete - the role " + pruneRoleName + " is being assigned to " + finalCount + " members.").queue();
				if (finalCount > members.size()/10) {
					config.getLogChannel().sendMessage("# WARNING\nThis prune affects a significant portion of all members!").queue();
				}
			});
		});

		return Responses.success(event, "Prune Started", "The prune action has started. Please check the log channel for information on the status of the prune.");

	}
}
