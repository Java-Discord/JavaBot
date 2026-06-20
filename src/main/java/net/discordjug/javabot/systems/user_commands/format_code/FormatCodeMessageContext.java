package net.discordjug.javabot.systems.user_commands.format_code;

import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.StringUtils;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import xyz.dynxsty.dih4jda.interactions.commands.application.ContextCommand;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
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
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull MessageContextInteractionEvent event) {
		String sanitized = StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw());

		if (sanitized.isBlank()) {
			event.reply("There is no code to format in that message.")
					.setEphemeral(true)
					.queue();
			return;
		}

		// Currently we always format as Java. A language dropdown will be added in the future.
		Code code = new Code(Language.JAVA, sanitized);
		List<String> messages = code.toDiscordMessages();

		// The reply both acknowledges the interaction and hands users the full,
		// un-split code as a downloadable file (so chunking never loses anything).
		FileUpload file = FileUpload.fromData(
				sanitized.getBytes(StandardCharsets.UTF_8),
				"code." + code.getLanguage().getDiscordName()
		);

		MessageChannel channel = event.getChannel();

		event.replyFiles(file)
				.setAllowedMentions(List.of())
				.queue(
						success -> sendChunksInOrder(channel, messages, 0, event),
						error -> {
							ExceptionLogger.capture(error, getClass().getSimpleName());
							Responses.error(event.getHook(), "The message could not be converted into a formatted code block.")
									.queue();
						}
				);
	}

	/**
	 * Sends the code-block chunks one at a time — each in the success callback of
	 * the previous — so Discord keeps them in order.
	 */
	private void sendChunksInOrder(MessageChannel channel, List<String> messages, int index,@Nonnull MessageContextInteractionEvent event) {
		if (index >= messages.size()) {
			return;
		}
		channel.sendMessage(messages.get(index))
				.setAllowedMentions(List.of()) // never ping people from pasted code
				.queue(
						success -> sendChunksInOrder(channel, messages, index + 1, event),
						error ->  {
							ExceptionLogger.capture(error, getClass().getSimpleName());
							Responses.error(event.getHook(), "The message could not be converted into a formatted code block.")
									.queue();
						}
				);
	}
}
