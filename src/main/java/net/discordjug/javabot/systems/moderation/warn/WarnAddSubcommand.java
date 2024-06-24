package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerateUserCommand;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.model.WarnSeverity;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn add command.</h3>
 * This Subcommand allows staff-members to add a single warn to any user.
 */
public class WarnAddSubcommand extends SlashCommand.Subcommand {
	private final BotConfig botConfig;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public WarnAddSubcommand(BotConfig botConfig, ModerationService moderationService) {
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		setCommandData(new SubcommandData("add", "Sends a warning to a user, and increases their warn severity rating.")
				.addOptions(
						new OptionData(OptionType.USER, "user", "The user to warn.", true),
						new OptionData(OptionType.STRING, "severity", "How severe was the offense?", true)
								.addChoice("Low", "LOW")
								.addChoice("Medium", "MEDIUM")
								.addChoice("High", "HIGH"),
						new OptionData(OptionType.STRING, "reason", "The reason for this user's warning.", true),
						new OptionData(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the warn is issued.")
				)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userMapping = event.getOption("user");
		OptionMapping reasonMapping = event.getOption("reason");
		OptionMapping severityMapping = event.getOption("severity");
		if (userMapping == null || reasonMapping == null || severityMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if(!Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		User target = userMapping.getAsUser();
		WarnSeverity severity = WarnSeverity.valueOf(severityMapping.getAsString().trim().toUpperCase());
		if (target.isBot()) {
			Responses.warning(event, "You cannot warn bots.").queue();
			return;
		}
		boolean quiet = ModerateUserCommand.isQuiet(botConfig, event);
		moderationService.warn(target, severity, reasonMapping.getAsString(), event.getMember(), event.getChannel(), quiet);
		Responses.success(event, "User Warned", "%s has been successfully warned.", target.getAsMention()).queue();
	}
}

