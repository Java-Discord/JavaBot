package net.javadiscord.javabot.systems.user_commands.format_code;

import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <h3>This class represents the "Format Code" Message Context command.</h3>
 */
public class FormatCodeMessageContext extends ContextCommand.Message {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 */
	public FormatCodeMessageContext() {
		setCommandData(Commands.message("Format Code")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		event.replyFormat("```java\n%s\n```", StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()))
				.setAllowedMentions(List.of())
				.setComponents(FormatCodeCommand.buildActionRow(event.getTarget(), event.getUser().getIdLong()))
				.queue();
	}
}
