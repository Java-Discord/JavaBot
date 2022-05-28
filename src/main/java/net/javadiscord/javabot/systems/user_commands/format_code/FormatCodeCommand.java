package net.javadiscord.javabot.systems.user_commands.format_code;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.util.InteractionUtils;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Command that allows members to format messages.
 */
// TODO: Needs Testing
public class FormatCodeCommand extends SlashCommand {
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		OptionMapping idOption = event.getOption("message-id");
		String format = event.getOption("format", "java", OptionMapping::getAsString);
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
							event.getHook().sendMessageFormat("```%s\n%s\n```", format, StringUtils.standardSanitizer().compute(target.getContentRaw()))
									.allowedMentions(List.of())
									.addActionRows(buildActionRow(target))
									.queue();
						} else {
							Responses.error(event.getHook(), "Could not find message; please specify a message id.").queue();
						}
					});
		} else {
			long messageId = idOption.getAsLong();
			event.getTextChannel().retrieveMessageById(messageId).queue(
					target -> event.getHook().sendMessageFormat("```%s\n%s\n```", format, StringUtils.standardSanitizer().compute(target.getContentRaw()))
							.allowedMentions(List.of())
							.addActionRows(buildActionRow(target))
							.queue(),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
		}
	}

	protected static ActionRow buildActionRow(Message target) {
		return ActionRow.of(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"),
				Button.link(target.getJumpUrl(), "View Original"));
	}
}
