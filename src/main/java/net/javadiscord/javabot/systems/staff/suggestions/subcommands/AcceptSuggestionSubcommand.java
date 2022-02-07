package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Subcommand that lets staff members accept suggestions.
 */
@Slf4j
public class AcceptSuggestionSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		var messageIdOption = event.getOption("message-id");
		if (messageIdOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var messageId = messageIdOption.getAsString();
		var config = Bot.config.get(event.getGuild());
		config.getModeration().getSuggestionChannel().retrieveMessageById(messageId).queue(m -> {
			var embed = m.getEmbeds().get(0);
			m.clearReactions().queue();
			var acceptEmbed = buildSuggestionAcceptEmbed(event.getUser(), embed, config);
			m.editMessageEmbeds(acceptEmbed).queue(
					message -> message.addReaction(config.getEmote().getSuccessEmote()).queue(),
					error -> Responses.error(event, error.getMessage()).queue());
		}, e -> log.error("Could not find suggestion message with id {}", messageId));
		return Responses.success(event, "Suggestion accepted",
				String.format("Successfully accepted suggestion with id `%s`", messageId));
	}

	private MessageEmbed buildSuggestionAcceptEmbed(User user, MessageEmbed embed, GuildConfig config) {
		return new EmbedBuilder()
				.setColor(config.getSlashCommand().getSuccessColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Accepted")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Accepted by " + user.getAsTag())
				.build();
	}
}