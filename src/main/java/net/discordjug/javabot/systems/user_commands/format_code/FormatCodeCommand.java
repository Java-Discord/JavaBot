package net.discordjug.javabot.systems.user_commands.format_code;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import org.jetbrains.annotations.NotNull;

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
						formatOption(),
						new OptionData(OptionType.STRING,"auto-indent","The type of indentation applied to the message, does not automatically indent if left blank.",false)
								.addChoice("Four Spaces","FOUR_SPACES")
								.addChoice("Two Spaces","TWO_SPACES")
								.addChoice("Tabs","TABS")
				)
		);
	}

	/**
	 * Builds the {@code format} option, generating one choice per {@link Language} (excluding
	 * {@link Language#UNKNOWN}) so the enum stays the single source of truth for the language list.
	 *
	 * @return the configured {@code format} option
	 */
	private static OptionData formatOption() {
		OptionData option = new OptionData(OptionType.STRING, "format", "The language used to format the code, defaults to Java if left blank.", false);
		for (Language language : Language.values()) {
			if (language != Language.UNKNOWN) {                       // UNKNOWN is the fallback, not a real choice
				option.addChoice(language.getDisplayName(), language.name()); // value = enum name so valueOf() reverses it
			}
		}
		return option;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idOption = event.getOption("message-id");
		Language language = event.getOption("format", Language.JAVA, o -> Language.fromString(o.getAsString()));
		String indentation = event.getOption("auto-indent","NULL",OptionMapping::getAsString);

		if (idOption == null) {
			event.getChannel().getHistory()
					.retrievePast(10)
					.queue(messages -> {
						Message target = messages.stream()
								.filter(m -> !m.getAuthor().isBot()).findFirst()
								.orElse(null);
						if (target != null) {
							sendFormattedCode(event, target, language, indentation);
						} else {
							Responses.errorWithTitle(event, "Message Not Found", "No recent user message could be found. Please specify a message ID.")
									.queue();
						}
					});
		} else {
			if (Checks.isInvalidLongInput(idOption)) {
				Responses.errorWithTitle(event, "Invalid Message ID", "Please provide a valid Discord message ID.")
				.queue();
				return;
			}
			long messageId = idOption.getAsLong();
			event.getChannel().retrieveMessageById(messageId).queue(
					target -> sendFormattedCode(event, target, language, indentation),
					error -> Responses.errorWithTitle(event, "Message Not Found", "Could not retrieve the message with ID `" + messageId + "`. Make sure the message exists and is accessible.").queue());
		}
	}

	private void sendFormattedCode(SlashCommandInteractionEvent event, Message target, Language language, String indentation) {
		String content = IndentationHelper.formatIndentation(
				StringUtils.standardSanitizer().compute(target.getContentRaw()),
				IndentationHelper.IndentationType.valueOf(indentation));

		Code code = new Code(language,content);

		FormatCodeDispatcher.sendCode(code, event, target);
	}
}
