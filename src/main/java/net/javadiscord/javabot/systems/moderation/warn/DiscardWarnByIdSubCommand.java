package net.javadiscord.javabot.systems.moderation.warn;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.moderation.ModerationService;
import net.javadiscord.javabot.systems.moderation.warn.dao.WarnRepository;
import net.javadiscord.javabot.systems.notification.NotificationService;
import net.javadiscord.javabot.util.Responses;

import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn discard-by-id command.</h3>
 */
public class DiscardWarnByIdSubCommand extends SlashCommand.Subcommand {
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param warnRepository DAO for interacting with the set of {@link Warn} objects.
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public DiscardWarnByIdSubCommand(NotificationService notificationService, BotConfig botConfig, WarnRepository warnRepository, ExecutorService asyncPool) {
		this.notificationService = notificationService;
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
		setSubcommandData(new SubcommandData("discard-by-id", "Discards a single warn, based on its id.")
				.addOption(OptionType.INTEGER, "id", "The warn's unique identifier.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idMapping = event.getOption("id");
		if (idMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		int id = idMapping.getAsInt();
		ModerationService service = new ModerationService(notificationService, botConfig, event, warnRepository, asyncPool);
		if (service.discardWarnById(id, event.getUser())) {
			Responses.success(event, "Warn Discarded", "Successfully discarded the specified warn with id `%s`", id).queue();
		} else {
			Responses.error(event, "Could not find and/or discard warn with id `%s`", id).queue();
		}
	}
}

