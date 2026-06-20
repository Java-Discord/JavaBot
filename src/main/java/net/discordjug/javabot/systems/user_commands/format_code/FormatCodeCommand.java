package net.discordjug.javabot.systems.user_commands.format_code;

import net.dv8tion.jda.api.interactions.InteractionHook;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.*;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * <h3>This class represents the /format-code command.</h3>
 */
public class FormatCodeCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public FormatCodeCommand() {
		setCommandData(Commands.slash("format-code", "Format unformatted code from a message")
				.setContexts(InteractionContextType.GUILD)
				.addOptions(
						new OptionData(OptionType.STRING, "message-id", "Message to be formatted, last message used if left blank.", false),
						new OptionData(OptionType.STRING, "format", "The language used to format the code, defaults to Java if left blank.", false)
								.addChoice("C", "c")
								.addChoice("C#", "csharp")
								.addChoice("C++", "cpp")
								.addChoice("CSS", "css")
								.addChoice("D", "d")
								.addChoice("Go", "go")
								.addChoice("HTML", "html")
								.addChoice("Java", "java")
								.addChoice("JavaScript", "js")
								.addChoice("Kotlin", "kotlin")
								.addChoice("PHP", "php")
								.addChoice("Python", "python")
								.addChoice("Ruby", "ruby")
								.addChoice("Rust", "rust")
								.addChoice("SQL", "sql")
								.addChoice("Swift", "swift")
								.addChoice("TypeScript", "typescript")
								.addChoice("XML", "xml"),
						new OptionData(OptionType.STRING,"auto-indent","The type of indentation applied to the message, does not automatically indent if left blank.",false)
								.addChoice("Four Spaces","FOUR_SPACES")
								.addChoice("Two Spaces","TWO_SPACES")
								.addChoice("Tabs","TABS")
				)
		);
	}

	@Contract("_ -> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target, long requesterId) {
		return ActionRow.of(InteractionUtils.createDeleteButton(requesterId),
				Button.link(target.getJumpUrl(), "View Original"));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idOption = event.getOption("message-id");
		String format = event.getOption("format", "java", OptionMapping::getAsString);
		String indentation = event.getOption("auto-indent","NULL",OptionMapping::getAsString);
		event.deferReply().queue();
		if (idOption == null) {
			event.getChannel().getHistory()
					.retrievePast(10)
					.queue(messages -> {
						Collections.reverse(messages);
						Message target = messages.stream()
								.filter(m -> !m.getAuthor().isBot()).findFirst()
								.orElse(null);
						if (target != null) {
							sendFormattedCode(event, target, format, indentation);
						} else {
							Responses.error(event.getHook(), "Could not find message; please specify a message id.").queue();
						}
					});
		} else {
			if (Checks.isInvalidLongInput(idOption)) {
				Responses.error(event.getHook(), "Please provide a valid message id!").queue();
				return;
			}
			long messageId = idOption.getAsLong();
			event.getChannel().retrieveMessageById(messageId).queue(
					target -> sendFormattedCode(event, target, format, indentation),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
		}
	}

	private void sendFormattedCode(SlashCommandInteractionEvent event, Message target, String format, String indentation) {
		String content = IndentationHelper.formatIndentation(
				StringUtils.standardSanitizer().compute(target.getContentRaw()),
				IndentationHelper.IndentationType.valueOf(indentation));

		if (content.isBlank()) {
			Responses.error(event.getHook(), "There is no code to format in that message.").queue();
			return;
		}

		Code code = new Code(Language.fromString(format), content);
		sendChunksInOrder(event.getHook(), code.toDiscordMessages(), 0);
	}

	private void sendChunksInOrder(InteractionHook hook, List<String> messages, int index) {
		if (index >= messages.size()) {
			return;
		}
		var action = hook.sendMessage(messages.get(index)).setAllowedMentions(List.of());

		action.queue(
				success -> sendChunksInOrder(hook, messages, index + 1),
				error ->  {
					ExceptionLogger.capture(error, getClass().getSimpleName());
					Responses.error(hook, "The message could not be converted into a formatted code block.")
							.queue();
				}
		);
	}
}