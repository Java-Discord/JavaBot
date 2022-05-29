package net.javadiscord.javabot.systems.moderation.report;

import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.Responses;

public class ReportUserContext extends ContextCommand.User {
	public ReportUserContext() {
		setCommandData(Commands.user("Report User"));
	}

	@Override
	public void execute(UserContextInteractionEvent event) {
		if (event.getTarget().equals(event.getUser())) {
			Responses.error(event, "You cannot perform this action on yourself.").queue();
			return;
		}
		event.replyModal(new ReportManager(event.getUser()).buildUserReportModal(event)).queue();
	}
}
