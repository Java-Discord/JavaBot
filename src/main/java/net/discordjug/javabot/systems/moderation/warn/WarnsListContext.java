package net.discordjug.javabot.systems.moderation.warn;

import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.discordjug.javabot.systems.moderation.ModerationService;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.concurrent.ExecutorService;

import org.springframework.dao.DataAccessException;

/**
 * <h3>This class represents the "Show Warns" User Context Menu command.</h3>
 * This Command allows users to see all their active warns.
 */
public class WarnsListContext extends ContextCommand.User {
	private final ExecutorService asyncPool;
	private final ModerationService moderationService;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 * @param asyncPool The main thread pool for asynchronous operations
	 * @param moderationService Service object for moderating members
	 */
	public WarnsListContext(ExecutorService asyncPool, ModerationService moderationService) {
		this.asyncPool = asyncPool;
		this.moderationService = moderationService;
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
		asyncPool.execute(() -> {
			try {
				event.getHook().sendMessageEmbeds(WarnsListCommand.buildWarnsEmbed(moderationService.getTotalSeverityWeight(event.getGuild(), event.getTarget().getIdLong()), event.getTarget())).queue();
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, WarnsListContext.class.getSimpleName());
			}
		});
	}

}
