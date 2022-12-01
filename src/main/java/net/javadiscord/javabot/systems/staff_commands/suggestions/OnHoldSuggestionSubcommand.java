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
 * <h3>This class represents the /suggestion on-hold command.</h3>
 */
public class OnHoldSuggestionSubcommand extends SuggestionSubcommand {

	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public OnHoldSuggestionSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("on-hold", "Marks a single suggestion as \"On Hold\".")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to mark as \"On Hold\".", true)
		);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message, GuildConfig config) {
		MessageEmbed embed = message.getEmbeds().get(0);
		MessageEmbed onHoldEmbed = buildSuggestionAcceptEmbed(event.getUser(), embed, config);
		message.editMessageEmbeds(onHoldEmbed).queue(
				edit -> edit.addReaction(botConfig.getSystems().getEmojiConfig().getClockEmoji()).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion On Hold", "Successfully marked suggestion with id `%s` as On Hold", message.getId())
				.setComponents(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionAcceptEmbed(@NotNull User user, @NotNull MessageEmbed embed, @NotNull GuildConfig config) {
		return new EmbedBuilder()
				.setColor(Responses.Type.WARN.getColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion On Hold")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Suggestion marked as On Hold by " + user.getAsTag())
				.build();
	}
}