package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.staff.suggestions.SuggestionSubcommand;

/**
 * Subcommand that lets staff members decline suggestions.
 */
public class DeclineSuggestionSubcommand extends SuggestionSubcommand {
	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(SlashCommandInteractionEvent event, Message message, GuildConfig config) {
		String reason = event.getOption("reason", null, OptionMapping::getAsString);
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed declineEmbed = this.buildSuggestionDeclineEmbed(event.getUser(), embed, reason, config);
		message.editMessageEmbeds(declineEmbed).queue(
				edit -> edit.addReaction(config.getEmote().getFailureEmote()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Declined", String.format("Successfully declined suggestion with id `%s`", message.getId()))
				.addActionRows(this.getJumpButton(message));
	}

	private MessageEmbed buildSuggestionDeclineEmbed(User user, MessageEmbed embed, String reason, GuildConfig config) {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(config.getSlashCommand().getErrorColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Declined")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Declined by " + user.getAsTag());
		if (reason != null) builder.addField("Reason", reason, false);
		return builder.build();
	}
}