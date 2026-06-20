package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.IndentationHelper;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
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
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		String indented = IndentationHelper.formatIndentation(
				StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()),
				IndentationHelper.IndentationType.TABS);

		if (indented.isBlank()) {
			event.reply("There is no code to format in that message.").setEphemeral(true).queue();
			return;
		}

		Code code = new Code(Language.JAVA, indented);
		List<String> messages = code.toDiscordMessages();

		// Reply with the full code as a file (acknowledges the interaction), then post
		// the readable code-block chunks in order.
		FileUpload file = FileUpload.fromData(indented.getBytes(StandardCharsets.UTF_8),
				"code." + code.getLanguage().getDiscordName());
		MessageChannel channel = event.getChannel();
		event.replyFiles(file)
				.setAllowedMentions(List.of())
				.queue(
						success -> sendChunksInOrder(channel, messages, 0, event),
						error ->  {
							ExceptionLogger.capture(error, getClass().getSimpleName());
							Responses.error(event.getHook(), "The message could not be converted into a formatted code block.")
									.queue();
						}
				);
	}

	private void sendChunksInOrder(MessageChannel channel, List<String> messages, int index, @Nonnull MessageContextInteractionEvent event) {
		if (index >= messages.size()) {
			return;
		}
		channel.sendMessage(messages.get(index))
				.setAllowedMentions(List.of())
				.queue(
						success -> sendChunksInOrder(channel, messages, index + 1, event),
						error -> {
							ExceptionLogger.capture(error, getClass().getSimpleName());
							Responses.error(event.getHook(), "The message could not be converted into a formatted code block.")
									.queue();
						}
				);
	}
}
