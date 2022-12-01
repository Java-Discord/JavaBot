package net.javadiscord.javabot.systems.staff_commands.suggestions;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members accept suggestions.
 */
public class AcceptSuggestionSubcommand extends SuggestionSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public AcceptSuggestionSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("accept", "Accepts a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to accept.", true)
		);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		MessageEmbed declineEmbed = buildSuggestionAcceptEmbed(event.getUser(), embed, config);
		message.editMessageEmbeds(declineEmbed).queue(
				edit -> edit.addReaction(botConfig.getSystems().getEmojiConfig().getSuccessEmote(event.getJDA())).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Accepted", "Successfully accepted suggestion with id `%s`", message.getId())
				.setComponents(getJumpButton(message));
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