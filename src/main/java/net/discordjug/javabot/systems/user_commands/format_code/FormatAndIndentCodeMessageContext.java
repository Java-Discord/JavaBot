package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.IndentationHelper;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;


/**
 * <h3>This class represents the "Format and Indent Code" Message Context command.</h3>
 */
public class FormatAndIndentCodeMessageContext extends ContextCommand.Message {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.CommandData}.
	 */
	public FormatAndIndentCodeMessageContext() {
		setCommandData(Commands.message("Format and Indent Code")
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		String indented = IndentationHelper.formatIndentation(
				StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()),
				IndentationHelper.IndentationType.TABS);

		Code code = new Code(Language.JAVA, indented);

		event.deferReply().queue(_ -> FormatCodeDispatcher.sendCode(code, event, event.getTarget()));
	}
}
