package net.javadiscord.javabot.systems.staff.suggestions;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.GuildConfig;

/**
 * Abstract parent class for all Suggestion subcommands, which handles the standard
 * behavior of retrieving the {@link Message} by its given id.
 */
public abstract class SuggestionSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			return Responses.warning(event, "This command can only be used in the context of a guild.");
		}
		GuildConfig config = Bot.config.get(event.getGuild());
		TextChannel suggestionChannel = config.getModeration().getSuggestionChannel();
		if (event.getChannelType() != ChannelType.TEXT || !event.getTextChannel().equals(suggestionChannel)) {
			return Responses.warning(event, "This command can only be used in " + suggestionChannel.getAsMention());
		}
		OptionMapping messageIdOption = event.getOption("message-id");
		if (messageIdOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		long messageId = messageIdOption.getAsLong();
		event.getMessageChannel().retrieveMessageById(messageId).queue(
				message -> this.handleSuggestionCommand(event, message, config).queue(),
				e -> Responses.error(event.getHook(), "Could not find suggestion message with id " + messageId));
		return event.deferReply(true);
	}

	protected abstract WebhookMessageAction<Message> handleSuggestionCommand(SlashCommandInteractionEvent event, Message message, GuildConfig config);

	protected ActionRow getJumpButton(Message m) {
		return ActionRow.of(Button.link(m.getJumpUrl(), "Jump to Message"));
	}
}
