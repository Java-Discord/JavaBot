package net.javadiscord.javabot.systems.user_commands.format_code;

import com.dynxsty.dih4jda.interactions.commands.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.StringUtils;

import java.util.List;

public class FormatCodeMessageContext extends ContextCommand.Message {
	public FormatCodeMessageContext() {
		setCommandData(Commands.message("Format Code"));
	}

	@Override
	public void execute(MessageContextInteractionEvent event) {
		event.replyFormat("```java\n%s\n```", StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()))
				.allowedMentions(List.of())
				.addActionRows(FormatCodeCommand.buildActionRow(event.getTarget()))
				.queue();
	}
}
