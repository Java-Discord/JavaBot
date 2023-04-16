package net.javadiscord.javabot.systems.user_commands.format_code;

import net.javadiscord.javabot.util.*;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
				.setGuildOnly(true)
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
								.addChoice("Four_Spaces","four")
								.addChoice("Two_Spaces","two")
								.addChoice("Tabs","tab")
				)
		);
	}

	@Contract("_ -> new")
	static @NotNull ActionRow buildActionRow(@NotNull Message target) {
		return ActionRow.of(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"),
				Button.link(target.getJumpUrl(), "View Original"));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping idOption = event.getOption("message-id");
		String format = event.getOption("format", "java", OptionMapping::getAsString);
		String indentation = event.getOption("auto-indent","null",OptionMapping::getAsString).toUpperCase();
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
							event.getHook().sendMessageFormat("```%s\n%s\n```", format, IndentationHelper.formatIndentation(StringUtils.standardSanitizer().compute(target.getContentRaw()),IndentationHelper.IndentationType.valueOf(indentation)))
									.setAllowedMentions(List.of())
									.setComponents(buildActionRow(target))
									.queue();
						} else {
							Responses.error(event.getHook(), "Could not find message; please specify a message id.").queue();
						}
					});
		} else {
			if (Checks.isInvalidLongInput(idOption)) {
				Responses.error(event, "Please provide a valid message id!").queue();
				return;
			}
			long messageId = idOption.getAsLong();
			event.getChannel().retrieveMessageById(messageId).queue(
					target -> event.getHook().sendMessageFormat("```%s\n%s\n```", format, IndentationHelper.formatIndentation(StringUtils.standardSanitizer().compute(target.getContentRaw()), IndentationHelper.IndentationType.valueOf(indentation)))
							.setAllowedMentions(List.of())
							.setComponents(buildActionRow(target))
							.queue(),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
		}
	}
}
