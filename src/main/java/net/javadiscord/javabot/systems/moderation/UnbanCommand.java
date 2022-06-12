package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.moderation.ModerateCommand;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;

/**
 * Command that allows staff-members to unban users from the current guild by their id.
 */
public class UnbanCommand extends ModerateCommand {
	public UnbanCommand() {
		setSlashCommandData(Commands.slash("unban", "Unbans a member")
				.addOption(OptionType.STRING, "id", "The ID of the user you want to unban", true)
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
	}

	@Override
	protected ReplyCallbackAction handleModerationCommand(SlashCommandInteractionEvent event, Member commandUser) {
		OptionMapping idOption = event.getOption("id");
		if (idOption == null || !Checks.checkLongInput(idOption)) {
			return Responses.error(event, "Missing required arguments.");
		}
		long id = idOption.getAsLong();
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService moderationService = new ModerationService(event.getInteraction());
		if (moderationService.unban(id, event.getMember(), event.getTextChannel(), quiet)) {
			return Responses.success(event, "User Unbanned", String.format("User with id `%s` has been unbanned.", id));
		} else {
			return Responses.warning(event, String.format("Could not find banned User with id `%s`", id));
		}
	}
}