package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.StringUtils;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;


/**
 * <h3>This class represents the "Format Code" Message Context command.</h3>
 */
public class FormatCodeMessageContext extends ContextCommand.Message {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 */
	public FormatCodeMessageContext() {
		setCommandData(Commands.message("Format Code")
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		String content = StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw());

		Code code = new Code(Language.JAVA, content);

		FormatCodeDispatcher.sendCode(code, event, event.getTarget());
	}
}
