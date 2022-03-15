package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.IMessageContextCommand;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;
import java.util.Collections;

/**
 * Command that allows members to format messages.
 */
public class FormatCodeCommand implements ISlashCommand, IMessageContextCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var idOption = event.getOption("message-id");
		String format = event.getOption("format", "java", OptionMapping::getAsString);
		var slashConfig = Bot.config.get(event.getGuild()).getSlashCommand();
		if (idOption == null) {
			event.getChannel().getHistory()
				.retrievePast(10)
				.queue(messages -> {
					Message target = null;
					Collections.reverse(messages);
					for (Message m : messages) {
						if (!m.getAuthor().isBot()) target = m;
					}
					if (target != null) event.getHook().sendMessageEmbeds(buildFormatCodeEmbed(target, format, slashConfig)).queue();
					else Responses.error(event.getHook(), "Missing required arguments.").queue();
				});
		} else {
			long messageId = idOption.getAsLong();
			event.getTextChannel().retrieveMessageById(messageId).queue(
					m -> event.getHook().sendMessageEmbeds(buildFormatCodeEmbed(m, format, slashConfig)).queue(),
					e -> Responses.error(event.getHook(), "Could not retrieve message with id: " + messageId).queue());
		}
		return event.deferReply();
	}

	@Override
	public ReplyCallbackAction handleMessageContextCommandInteraction(MessageContextInteractionEvent event) {
		return event.replyEmbeds(buildFormatCodeEmbed(event.getTarget(), "java", Bot.config.get(event.getGuild()).getSlashCommand()));
	}

	private MessageEmbed buildFormatCodeEmbed(Message message, String format, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(message.getAuthor().getAsTag(), null, message.getAuthor().getEffectiveAvatarUrl())
				.setTitle("Original Message", message.getJumpUrl())
				.setColor(config.getDefaultColor())
				.setDescription(String.format("```%s\n%s\n```", format, message.getContentRaw()))
				.setFooter("Formatted as: " + format)
				.setTimestamp(Instant.now())
				.build();
	}
}
