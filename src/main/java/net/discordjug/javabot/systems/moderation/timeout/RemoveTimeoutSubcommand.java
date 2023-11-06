package net.discordjug.javabot.systems.moderation.timeout;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerateUserCommand;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /timeout remove command.</h3>
 * This Subcommand allows staff-members to manually remove a timeout.
 */
public class RemoveTimeoutSubcommand extends TimeoutSubcommand {
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param warnRepository The main thread pool for asynchronous operations
	 */
	public RemoveTimeoutSubcommand(NotificationService notificationService, BotConfig botConfig, ExecutorService asyncPool, WarnRepository warnRepository) {
		this.notificationService = notificationService;
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
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
		ModerationService service = new ModerationService(notificationService, botConfig.get(event.getGuild()), warnRepository, asyncPool);
		service.removeTimeout(member, reasonOption.getAsString(), event.getMember(), channel, quiet);
		return Responses.success(event, "Timeout Removed", "%s's timeout has been removed.", member.getAsMention());
	}
}
