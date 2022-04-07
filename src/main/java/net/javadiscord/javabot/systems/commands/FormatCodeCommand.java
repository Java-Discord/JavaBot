package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.MessageContextCommand;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.util.InteractionUtils;
import net.javadiscord.javabot.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Command that allows members to format messages.
 */
public class FormatCodeCommand implements SlashCommand, MessageContextCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var idOption = event.getOption("message-id");
		String format = event.getOption("format", "java", OptionMapping::getAsString);
		if (idOption == null) {
			event.getChannel().getHistory()
					.retrievePast(10)
					.queue(messages -> {
						Message target = null;
						Collections.reverse(messages);
						for (Message m : messages) {
							if (!m.getAuthor().isBot()) target = m;
						}
						if (target != null) {
							event.getHook().sendMessageFormat("```%s\n%s\n```", format, StringUtils.standardSanitizer().compute(target.getContentRaw()))
									.allowedMentions(List.of())
									.addActionRows(this.buildActionRow(target))
									.queue();
						} else {
							Responses.error(event.getHook(), "Missing required arguments.").queue();
						}
					});
		} else {
			long messageId = idOption.getAsLong();
			event.getTextChannel().retrieveMessageById(messageId).queue(
					m -> event.getHook().sendMessageFormat("```%s\n%s\n```", format, StringUtils.standardSanitizer().compute(m.getContentRaw()))
							.allowedMentions(List.of())
							.addActionRows(this.buildActionRow(m))
							.queue(),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
		}
		return event.deferReply();
	}

	@Override
	public ReplyCallbackAction handleMessageContextCommandInteraction(MessageContextInteractionEvent event) {
		return event.replyFormat("```java\n%s\n```", StringUtils.standardSanitizer().compute(event.getTarget().getContentRaw()))
				.allowedMentions(List.of())
				.addActionRows(this.buildActionRow(event.getTarget()));
	}

	private ActionRow buildActionRow(Message target) {
		return ActionRow.of(Button.secondary(InteractionUtils.DELETE_ORIGINAL_TEMPLATE, "\uD83D\uDDD1️"), Button.link(target.getJumpUrl(), "View Original"));
	}
}
