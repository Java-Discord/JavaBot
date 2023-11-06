package net.discordjug.javabot.systems.user_commands.format_code;


import net.discordjug.javabot.util.IndentationHelper;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;

import java.util.List;

/**
 * <h3>This class represents the "Format and Indent Code" Message Context command.</h3>
 */
public class FormatAndIndentCodeMessageContext extends ContextCommand.Message {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 */
	public FormatAndIndentCodeMessageContext() {
		setCommandData(Commands.message("Format and Indent Code")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		event.replyFormat("```java\n%s\n```", IndentationHelper.formatIndentation(StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()), IndentationHelper.IndentationType.TABS))
				.setAllowedMentions(List.of())
				.setComponents(FormatCodeCommand.buildActionRow(event.getTarget(), event.getUser().getIdLong()))
				.queue();
	}
}
