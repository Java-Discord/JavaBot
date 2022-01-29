package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Subcommand that lets staff members decline suggestions.
 */
@Slf4j
public class DeclineSuggestionSubcommand implements SlashCommandHandler {
	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
		var messageIdOption = event.getOption("message-id");
		if (messageIdOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var messageId = messageIdOption.getAsString();
		var config = Bot.config.get(event.getGuild());
		config.getModeration().getSuggestionChannel().retrieveMessageById(messageId).queue(m -> {
			var embed = m.getEmbeds().get(0);
			m.clearReactions().queue();
			var declineEmbed = buildSuggestionDeclineEmbed(event.getUser(), embed, config);
			m.editMessageEmbeds(declineEmbed).queue(
					message -> message.addReaction(config.getEmote().getFailureEmote()).queue(),
					error -> Responses.error(event, error.getMessage()).queue());
		}, e -> log.error("Could not find suggestion message with id {}", messageId));
		return Responses.success(event, "Suggestion declined",
				String.format("Successfully declined suggestion with id `%s`", messageId));
	}

	private MessageEmbed buildSuggestionDeclineEmbed(User user, MessageEmbed embed, GuildConfig config) {
		return new EmbedBuilder()
				.setColor(config.getSlashCommand().getErrorColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Declined")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Declined by " + user.getAsTag())
				.build();
	}
}