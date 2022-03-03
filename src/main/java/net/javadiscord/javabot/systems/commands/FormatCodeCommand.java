package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.IMessageContextCommand;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows members to format messages.
 */
public class FormatCodeCommand implements ISlashCommand, IMessageContextCommand {
	private long id;

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var idOption = event.getOption("message-id");
		var formatOption = event.getOption("format");
		var slashConfig = Bot.config.get(event.getGuild()).getSlashCommand();
		String format = formatOption == null ? "java" : formatOption.getAsString();
		if(idOption == null) {
			if(event.getChannel().hasLatestMessage()) {
				event.getChannel().getHistory().retrievePast(10)
						.queue(messages -> {
							for(Message message : messages) {
								if(!message.getAuthor().isBot()) {
									id = message.getIdLong();
									break;
								}
							}
							event.getTextChannel().retrieveMessageById(id).queue(
									m -> event.getHook().sendMessageEmbeds(buildFormatCodeEmbed(m, m.getAuthor(), format, slashConfig)).queue(),
									e -> Responses.error(event.getHook(), "Could not retrieve message.").queue());
						});
			} else {
				return Responses.error(event, "Missing required arguments.");
			}
		} else {
			id = idOption.getAsLong();
			event.getTextChannel().retrieveMessageById(id).queue(
					m -> event.getHook().sendMessageEmbeds(buildFormatCodeEmbed(m, m.getAuthor(), format, slashConfig)).queue(),
					e -> Responses.error(event.getHook(), "Could not retrieve message.").queue());
		}
		return event.deferReply();
	}

	@Override
	public ReplyCallbackAction handleMessageContextCommandInteraction(MessageContextInteractionEvent event) {
		Message message = event.getTarget();
		return event.replyEmbeds(buildFormatCodeEmbed(message, message.getAuthor(), "java", Bot.config.get(event.getGuild()).getSlashCommand()));
	}

	private MessageEmbed buildFormatCodeEmbed(Message message, User author, String format, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("Original Message", message.getJumpUrl())
				.setColor(config.getDefaultColor())
				.setDescription(String.format("```%s\n%s\n```", format, message.getContentRaw()))
				.setFooter("Formatted as: " + format)
				.setTimestamp(Instant.now())
				.build();
	}
}
