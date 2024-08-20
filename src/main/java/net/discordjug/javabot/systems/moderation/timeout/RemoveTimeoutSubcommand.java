package net.discordjug.javabot.systems.moderation.timeout;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerateUserCommand;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /timeout remove command.</h3>
 * This Subcommand allows staff-members to manually remove a timeout.
 */
public class RemoveTimeoutSubcommand extends TimeoutSubcommand {
	private final BotConfig botConfig;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param moderationService Service object for moderating members
	 */
	public RemoveTimeoutSubcommand(BotConfig botConfig, ModerationService moderationService) {
		this.botConfig = botConfig;
		this.moderationService = moderationService;
		setCommandData(new SubcommandData("remove", "Removes a timeout from the specified server member.")
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
		boolean quiet = ModerateUserCommand.isQuiet(botConfig, event);
		if (!member.isTimedOut()) {
			return Responses.error(event, "Could not remove timeout from member %s; they're not timed out.", member.getAsMention());
		}
		moderationService.removeTimeout(member, reasonOption.getAsString(), event.getMember(), channel, quiet);
		return Responses.success(event, "Timeout Removed", "%s's timeout has been removed.", member.getAsMention());
	}
}
