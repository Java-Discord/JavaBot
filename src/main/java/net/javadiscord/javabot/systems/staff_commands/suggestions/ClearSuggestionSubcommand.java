package net.javadiscord.javabot.systems.staff_commands.suggestions;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.SystemsConfig;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand that lets staff members clear suggestions.
 */
public class ClearSuggestionSubcommand extends SuggestionSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public ClearSuggestionSubcommand() {
		setSubcommandData(new SubcommandData("clear", "Clears a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to clear.", true)
		);
	}

	@Override
	protected WebhookMessageAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		MessageEmbed clearEmbed = buildSuggestionClearEmbed(embed, config);
		SystemsConfig.EmojiConfig emojiConfig = Bot.getConfig().getSystems().getEmojiConfig();
		message.editMessageEmbeds(clearEmbed).queue(
				edit -> {
					edit.addReaction(emojiConfig.getUpvoteEmote(event.getJDA())).queue();
					edit.addReaction(emojiConfig.getDownvoteEmote(event.getJDA())).queue();
				},
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Cleared", "Successfully cleared suggestion with id `%s`", message.getId())
				.addActionRows(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionClearEmbed(@NotNull MessageEmbed embed, @NotNull GuildConfig config) {
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter(null)
				.build();
	}
}
