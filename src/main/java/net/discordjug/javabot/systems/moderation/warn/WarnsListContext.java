package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.moderation.warn.dao.WarnRepository;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

import org.springframework.dao.DataAccessException;

/**
 * <h3>This class represents the "Show Warns" User Context Menu command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListContext extends ContextCommand.User {
	private final BotConfig botConfig;
	private final ExecutorService asyncPool;
	private final WarnRepository warnRepository;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 * @param botConfig The main configuration of the bot
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param warnRepository DAO for interacting with the set of {@link net.discordjug.javabot.systems.moderation.warn.model.Warn} objects.
	 */
	public WarnsListContext(BotConfig botConfig, ExecutorService asyncPool, WarnRepository warnRepository) {
		this.botConfig = botConfig;
		this.asyncPool = asyncPool;
		this.warnRepository = warnRepository;
		setCommandData(Commands.user("Show Warns")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(UserContextInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		event.deferReply(false).queue();
		LocalDateTime cutoff = LocalDateTime.now().minusDays(botConfig.get(event.getGuild()).getModerationConfig().getWarnTimeoutDays());
		asyncPool.execute(() -> {
			try {
				event.getHook().sendMessageEmbeds(WarnsListCommand.buildWarnsEmbed(warnRepository.getActiveWarnsByUserId(event.getTarget().getIdLong(), cutoff), event.getTarget())).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, WarnsListContext.class.getSimpleName());
			}
		});
	}

}
