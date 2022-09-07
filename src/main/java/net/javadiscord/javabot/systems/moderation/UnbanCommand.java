package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /unban command.</h3>
 * This Command allows staff-members to unban users from the current guild by their id.
 */
public class UnbanCommand extends ModerateCommand {
	private final NotificationService notificationService;
	private final DbHelper dbHelper;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param dbHelper An object managing databse operations
	 */
	public UnbanCommand(NotificationService notificationService, BotConfig botConfig, DbHelper dbHelper) {
		super(botConfig);
		this.notificationService = notificationService;
		this.dbHelper = dbHelper;
		setModerationSlashCommandData(Commands.slash("unban", "Unbans a member")
				.addOption(OptionType.STRING, "id", "The id of the user you want to unban", true)
				.addOption(OptionType.STRING, "reason", "The reason for unbanning this user", true)
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
		boolean quiet = event.getOption("quiet", false, OptionMapping::getAsBoolean);
		ModerationService service = new ModerationService(notificationService, botConfig, event.getInteraction(), dbHelper);
		if (service.unban(id, reasonOption.getAsString(), event.getMember(), event.getChannel(), quiet)) {
			return Responses.success(event, "User Unbanned", "User with id `%s` has been unbanned.", id);
		} else {
			return Responses.warning(event, "Could not find banned User with id `%s`", id);
		}
	}
}