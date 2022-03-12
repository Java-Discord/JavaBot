package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.staff.suggestions.SuggestionSubcommand;

/**
 * Subcommand that lets staff members clear suggestions.
 */
@Slf4j
public class ClearSuggestionSubcommand extends SuggestionSubcommand {
	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(SlashCommandInteractionEvent event, Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed clearEmbed = this.buildSuggestionClearEmbed(embed, config);
		message.editMessageEmbeds(clearEmbed).queue(
				edit -> {
					edit.addReaction(config.getEmote().getUpvoteEmote()).queue();
					edit.addReaction(config.getEmote().getDownvoteEmote()).queue();
				},
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Cleared", String.format("Successfully cleared suggestion with id `%s`", message.getId()))
				.addActionRows(this.getJumpButton(message));
	}

	private MessageEmbed buildSuggestionClearEmbed(MessageEmbed embed, GuildConfig config) {
		return new EmbedBuilder()
				.setColor(config.getSlashCommand().getDefaultColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter(null)
				.build();
	}
}
