package net.javadiscord.javabot.systems.staff.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.systems.staff.suggestions.SuggestionSubcommand;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members mark suggestions as "On Hold".
 */
public class OnHoldSuggestionSubcommand extends SuggestionSubcommand {
	public OnHoldSuggestionSubcommand() {
		setSubcommandData(new SubcommandData("on-hold", "Marks a single suggestion as \"On Hold\".")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to mark as \"On Hold\".", true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed onHoldEmbed = buildSuggestionAcceptEmbed(event.getUser(), embed, config);
		message.editMessageEmbeds(onHoldEmbed).queue(
				edit -> edit.addReaction(config.getEmote().getClockEmoji()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion On Hold", String.format("Successfully marked suggestion with id `%s` as On Hold", message.getId()))
				.addActionRows(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionAcceptEmbed(@NotNull User user, @NotNull MessageEmbed embed, @NotNull GuildConfig config) {
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