package net.javadiscord.javabot.systems.staff_commands.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.systems.staff_commands.suggestions.SuggestionSubcommand;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members accept suggestions.
 */
public class AcceptSuggestionSubcommand extends SuggestionSubcommand {
	public AcceptSuggestionSubcommand() {
		setSubcommandData(new SubcommandData("accept", "Accepts a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to accept.", true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed declineEmbed = buildSuggestionAcceptEmbed(event.getUser(), embed, config);
		message.editMessageEmbeds(declineEmbed).queue(
				edit -> edit.addReaction(config.getEmote().getSuccessEmote()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Accepted", String.format("Successfully accepted suggestion with id `%s`", message.getId()))
				.addActionRows(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionAcceptEmbed(@NotNull User user, @NotNull MessageEmbed embed, @NotNull GuildConfig config) {
		return new EmbedBuilder()
				.setColor(Responses.Type.SUCCESS.getColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Accepted")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Accepted by " + user.getAsTag())
				.build();
	}
}