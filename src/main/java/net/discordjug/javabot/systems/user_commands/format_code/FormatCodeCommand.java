package net.discordjug.javabot.systems.user_commands.format_code;

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

	/**
	 * Builds the action row placed on the file-upload message: a delete button and a "View Original" link.
	 *
	 * @param target      the original message linked by the "View Original" button
	 * @param requesterId the id of the user permitted to delete the message
	 * @return an action row containing the delete and "View Original" buttons
	 */
	@Contract("_ -> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target, long requesterId) {
		return ActionRow.of(InteractionUtils.createDeleteButton(requesterId),
				Button.link(target.getJumpUrl(), "View Original"));
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
						Collections.reverse(messages);
						Message target = messages.stream()
								.filter(m -> !m.getAuthor().isBot()).findFirst()
								.orElse(null);
						if (target != null) {
							sendFormattedCode(event, target, language, indentation);
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
					target -> sendFormattedCode(event, target, language, indentation),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
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
