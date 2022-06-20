package net.javadiscord.javabot.systems.staff_commands.suggestions.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.systems.staff_commands.suggestions.SuggestionSubcommand;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members decline suggestions.
 */
public class DeclineSuggestionSubcommand extends SuggestionSubcommand {
	public DeclineSuggestionSubcommand() {
		setSubcommandData(new SubcommandData("decline", "Declines a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to decline.", true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		String reason = event.getOption("reason", null, OptionMapping::getAsString);
		MessageEmbed embed = message.getEmbeds().get(0);
		message.clearReactions().queue();
		MessageEmbed declineEmbed = buildSuggestionDeclineEmbed(event.getUser(), embed, reason, config);
		message.editMessageEmbeds(declineEmbed).queue(
				edit -> edit.addReaction(config.getEmote().getFailureEmote()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Declined", String.format("Successfully declined suggestion with id `%s`", message.getId()))
				.addActionRows(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionDeclineEmbed(@NotNull User user, @NotNull MessageEmbed embed, String reason, @NotNull GuildConfig config) {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Responses.Type.ERROR.getColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Declined")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Declined by " + user.getAsTag());
		if (reason != null) builder.addField("Reason", reason, false);
		return builder.build();
	}
}