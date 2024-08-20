package net.discordjug.javabot.systems.moderation;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /unban command.</h3>
 * This Command allows staff-members to unban users from the current guild by their id.
 */
public class UnbanCommand extends ModerateCommand {
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public UnbanCommand(BotConfig botConfig, ModerationService moderationService) {
		super(botConfig);
		this.moderationService = moderationService;
		setModerationSlashCommandData(Commands.slash("unban", "Unbans a member")
				.addOption(OptionType.STRING, "id", "The id of the user you want to unban", true)
				.addOption(OptionType.STRING, "reason", "The reason for unbanning this user", true)
				.addOption(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the unban is issued.", false)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator) {
		OptionMapping idOption = event.getOption("id");
		OptionMapping reasonOption = event.getOption("reason");
		if (idOption == null || Checks.isInvalidLongInput(idOption) || reasonOption == null) {
			return Responses.replyMissingArguments(event);
		}
		long id = idOption.getAsLong();
		boolean quiet = ModerateUserCommand.isQuiet(botConfig, event);
		if (moderationService.unban(id, reasonOption.getAsString(), event.getMember(), event.getChannel(), quiet)) {
			return Responses.success(event, "User Unbanned", "User with id `%s` has been unbanned.", id);
		} else {
			return Responses.warning(event, "Could not find banned User with id `%s`", id);
		}
	}
}