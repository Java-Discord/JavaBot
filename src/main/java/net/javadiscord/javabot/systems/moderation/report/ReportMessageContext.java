package net.javadiscord.javabot.systems.moderation.report;

import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.Responses;

/**
 * <h3>This class represents the "Report Message" Message Context Menu command.</h3>
 */
public class ReportMessageContext extends ContextCommand.Message {
	public ReportMessageContext() {
		setCommandData(Commands.message("Report Message"));
	}

	@Override
	public void execute(MessageContextInteractionEvent event) {
		if (event.getTarget().getAuthor().equals(event.getUser())) {
			Responses.error(event, "You cannot perform this action on yourself.").queue();
			return;
		}
		event.replyModal(new ReportManager(event.getUser()).buildMessageReportModal(event)).queue();
	}
}
