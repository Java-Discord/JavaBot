package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /unban command.</h3>
 * This Command allows staff-members to unban users from the current guild by their id.
 */
public class UnbanCommand extends ModerateCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public UnbanCommand() {
		setModerationSlashCommandData(Commands.slash("unban", "Unbans a member")
				.addOption(OptionType.STRING, "id", "The ID of the user you want to unban", true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member moderator) {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null || Checks.isInvalidLongInput(idOption)) {
			return Responses.replyMissingArguments(event);
		}
		long id = idOption.getAsLong();
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService moderationService = new ModerationService(event.getInteraction());
		if (moderationService.unban(id, event.getMember(), event.getChannel(), quiet)) {
			return Responses.success(event, "User Unbanned", "User with id `%s` has been unbanned.", id);
		} else {
			return Responses.warning(event, "Could not find banned User with id `%s`", id);
		}
	}
}