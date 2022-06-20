package net.javadiscord.javabot.systems.moderation.timeout.subcommands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.util.Responses;

/**
 * Subcommand that allows staff-members to manually remove a timeout.
 */
public class RemoveTimeoutSubcommand extends SlashCommand.Subcommand {
	public RemoveTimeoutSubcommand() {
		setSubcommandData(new SubcommandData("remove", "Removes a Timeout from the given User.")
				.addOptions(
						new OptionData(OptionType.USER, "user", "The user whose Timeout should be removed.", true),
						new OptionData(OptionType.STRING, "reason", "The reason for removing this Timeout.", true),
						new OptionData(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the Timeout is issued.", false)
				)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping userOption = event.getOption("user");
		OptionMapping reasonOption = event.getOption("reason");
		if (userOption == null || reasonOption == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		Member member = userOption.getAsMember();
		if (member == null) {
			Responses.error(event, "Cannot remove a timeout of a user who is not a member of this server").queue();
			return;
		}
		String reason = reasonOption.getAsString();
		MessageChannel channel = event.getMessageChannel();
		if (!channel.getType().isMessage()) {
			Responses.error(event, "This command can only be performed in a server message channel.").queue();
			return;
		}
		var quietOption = event.getOption("quiet");
		boolean quiet = quietOption != null && quietOption.getAsBoolean();

		if (!member.isTimedOut()) {
			Responses.error(event, String.format("Could not remove Timeout from member %s; they are not timed out.", member.getAsMention())).queue();
			return;
		}
		ModerationService service = new ModerationService(event);
		service.removeTimeout(member, reason, event.getMember(), channel, quiet);
		Responses.success(event, "Timeout Removed", String.format("%s's Timeout has been removed.", member.getAsMention())).queue();
	}
}
