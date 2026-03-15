package net.discordjug.javabot.systems.staff_commands.suggestions;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /suggestion decline command.</h3>
 */
public class DeclineSuggestionSubcommand extends SuggestionSubcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public DeclineSuggestionSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("decline", "Declines a single suggestion.")
				.addOption(OptionType.STRING, "message-id", "The message id of the suggestion you want to decline.", true)
		);
	}

	@Override
	protected WebhookMessageCreateAction<Message> handleSuggestionCommand(@NotNull SlashCommandInteractionEvent event, @NotNull Message message) {
		String reason = event.getOption("reason", null, OptionMapping::getAsString);
		MessageEmbed embed = message.getEmbeds().get(0);
		MessageEmbed declineEmbed = buildSuggestionDeclineEmbed(event.getUser(), embed, reason);
		message.editMessageEmbeds(declineEmbed).queue(
				edit -> edit.addReaction(botConfig.getSystems().getEmojiConfig().getFailureEmote(event.getJDA())).queue(),
				error -> Responses.error(event.getHook(), error.getMessage()).queue());
		return Responses.success(event.getHook(), "Suggestion Declined", "Successfully declined suggestion with id `%s`", message.getId())
				.setComponents(getJumpButton(message));
	}

	private @NotNull MessageEmbed buildSuggestionDeclineEmbed(@NotNull User user, @NotNull MessageEmbed embed, String reason) {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Responses.Type.ERROR.getColor())
				.setAuthor(embed.getAuthor().getName(), embed.getAuthor().getUrl(), embed.getAuthor().getIconUrl())
				.setTitle("Suggestion Declined")
				.setDescription(embed.getDescription())
				.setTimestamp(embed.getTimestamp())
				.setFooter("Declined by " + UserUtils.getUserTag(user));
		if (reason != null) builder.addField("Reason", reason, false);
		return builder.build();
	}
}