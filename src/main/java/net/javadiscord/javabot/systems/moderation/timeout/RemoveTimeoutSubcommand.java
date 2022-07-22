package net.javadiscord.javabot.systems.moderation.timeout;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /timeout remove command.</h3>
 * This Subcommand allows staff-members to manually remove a timeout.
 */
public class RemoveTimeoutSubcommand extends TimeoutSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public RemoveTimeoutSubcommand() {
		setSubcommandData(new SubcommandData("remove", "Removes a timeout from the specified server member.")
				.addOptions(
						new OptionData(OptionType.USER, "member", "The member whose timeout should be removed.", true),
						new OptionData(OptionType.STRING, "reason", "The reason for removing this timeout.", true),
						new OptionData(OptionType.BOOLEAN, "quiet", "If true, don't send a message in the server channel where the timeout-removal is issued.", false)
				)
		);
	}

	@Override
	protected ReplyCallbackAction handleTimeoutCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Member member) {
		OptionMapping reasonOption = event.getOption("reason");
		if (reasonOption == null) {
			return Responses.replyMissingArguments(event);
		}
		MessageChannel channel = event.getMessageChannel();
		if (!channel.getType().isMessage()) {
			return Responses.error(event, "This command can only be performed in a server message channel.");
		}
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		if (!member.isTimedOut()) {
			return Responses.error(event, "Could not remove timeout from member %s; they're not timed out.", member.getAsMention());
		}
		ModerationService service = new ModerationService(event);
		service.removeTimeout(member, reasonOption.getAsString(), event.getMember(), channel, quiet);
		return Responses.success(event, "Timeout Removed", "%s's timeout has been removed.", member.getAsMention());
	}
}
