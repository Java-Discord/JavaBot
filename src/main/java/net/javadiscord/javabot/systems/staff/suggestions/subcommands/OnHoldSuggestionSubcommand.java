package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.staff.suggestions.SuggestionSubcommand;

/**
 * Subcommand that lets staff members mark suggestions as "On Hold".
 */
@Slf4j
public class OnHoldSuggestionSubcommand extends SuggestionSubcommand {
	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(SlashCommandInteractionEvent event, Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed onHoldEmbed = this.buildSuggestionAcceptEmbed(event.getUser(), embed, config);
		message.editMessageEmbeds(onHoldEmbed).queue(
				edit -> edit.addReaction(config.getEmote().getClockEmoji()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion On Hold", String.format("Successfully marked suggestion with id `%s` as On Hold", message.getId()))
				.addActionRows(this.getJumpButton(message));
	}

	private MessageEmbed buildSuggestionAcceptEmbed(User user, MessageEmbed embed, GuildConfig config) {
		return new EmbedBuilder()
				.setColor(config.getSlashCommand().getWarningColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion On Hold")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Suggestion marked as On Hold by " + user.getAsTag())
				.build();
	}
}