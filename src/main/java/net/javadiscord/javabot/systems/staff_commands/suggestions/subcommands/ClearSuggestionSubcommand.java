package net.javadiscord.javabot.systems.staff_commands.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.systems.staff_commands.suggestions.SuggestionSubcommand;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members clear suggestions.
 */
public class ClearSuggestionSubcommand extends SuggestionSubcommand {
	public ClearSuggestionSubcommand() {
		setSubcommandData(new SubcommandData("clear", "Clears a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to clear.", true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed clearEmbed = buildSuggestionClearEmbed(embed, config);
		message.editMessageEmbeds(clearEmbed).queue(
				edit -> {
					edit.addReaction(config.getEmote().getUpvoteEmote()).queue();
					edit.addReaction(config.getEmote().getDownvoteEmote()).queue();
				},
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Cleared", String.format("Successfully cleared suggestion with id `%s`", message.getId()))
				.addActionRows(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionClearEmbed(@NotNull MessageEmbed embed, @NotNull GuildConfig config) {
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
