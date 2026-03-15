package net.discordjug.javabot.systems.moderation.report;

import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * <h3>This class represents the "Report User" User Context Menu command.</h3>
 */
public class ReportUserContext extends ContextCommand.User {
	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 * @param botConfig The injected {@link BotConfig}
	 */
	public ReportUserContext(BotConfig botConfig) {
		this.botConfig = botConfig;
		setCommandData(Commands.user("Report User")
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(UserContextInteractionEvent event) {
		if (event.getTarget().equals(event.getUser())) {
			Responses.error(event, "You cannot perform this action on yourself.").queue();
			return;
		}
		event.replyModal(new ReportManager(botConfig).buildUserReportModal(event)).queue();
	}
}
