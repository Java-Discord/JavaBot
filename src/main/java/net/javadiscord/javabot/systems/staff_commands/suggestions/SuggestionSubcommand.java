package net.javadiscord.javabot.systems.staff_commands.suggestions;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.data.config.GuildConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Abstract parent class for all Suggestion subcommands, which handles the standard
 * behavior of retrieving the {@link Message} by its given id.
 */
public abstract class SuggestionSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.warning(event, "This command can only be used in the context of a guild.").queue();
			return;
		}
		GuildConfig config = Bot.config.get(event.getGuild());
		TextChannel suggestionChannel = config.getModeration().getSuggestionChannel();
		if (event.getChannelType() != ChannelType.TEXT || !event.getTextChannel().equals(suggestionChannel)) {
			Responses.warning(event, "This command can only be used in " + suggestionChannel.getAsMention()).queue();
			return;
		}
		OptionMapping messageIdMapping = event.getOption("message-id");
		if (messageIdMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		if (!Checks.checkLongInput(messageIdMapping)) {
			Responses.error(event, "Please provide a valid message id.").queue();
			return;
		}
		long messageId = messageIdMapping.getAsLong();
		event.deferReply(true).queue();
		event.getMessageChannel().retrieveMessageById(messageId).queue(
				message -> handleSuggestionCommand(event, message, config).queue(),
				e -> Responses.error(event.getHook(), "Could not find suggestion message with id " + messageId));

	}

	protected abstract WebhookMessageAction<Message> handleSuggestionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Message message, GuildConfig config);

	protected ActionRow getJumpButton(@NotNull Message m) {
		return ActionRow.of(Button.link(m.getJumpUrl(), "Jump to Message"));
	}
}
