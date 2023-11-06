package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.systems.notification.NotificationService;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /warn discard-all command.</h3>
 */
public class DiscardAllWarnsSubcommand extends SlashCommand.Subcommand {
	private final NotificationService notificationService;
	private final BotConfig botConfig;
	private final WarnRepository warnRepository;
	private final ExecutorService asyncPool;

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param notificationService The {@link NotificationService}
	 * @param botConfig The main configuration of the bot
	 * @param warnRepository DAO for interacting with the set of {@link net.discordjug.javabot.systems.moderation.warn.model.Warn} objects.
	 * @param asyncPool The main thread pool for asynchronous operations
	 */
	public DiscardAllWarnsSubcommand(NotificationService notificationService, BotConfig botConfig, WarnRepository warnRepository, ExecutorService asyncPool) {
		this.notificationService = notificationService;
		this.botConfig = botConfig;
		this.warnRepository = warnRepository;
		this.asyncPool = asyncPool;
		setCommandData(new SubcommandData("discard-all", "Discards all warns from a single user.")
				.addOption(OptionType.USER, "user", "The user which warns should be discarded.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping userMapping = event.getOption("user");
		if (userMapping == null) {
			Responses.error(event, "Please provide a valid user.").queue();
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
		new ModerationService(notificationService, botConfig, event.getInteraction(), warnRepository, asyncPool).discardAllWarns(target, event.getMember());
		Responses.success(event, "Warns Discarded", "Successfully discarded all warns from **%s**.", UserUtils.getUserTag(target)).queue();
	}
}

