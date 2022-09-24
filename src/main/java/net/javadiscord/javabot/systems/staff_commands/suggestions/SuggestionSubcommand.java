package net.javadiscord.javabot.systems.staff_commands.suggestions;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
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
			Responses.replyGuildOnly(event).queue();
			return;
		}
		GuildConfig config = Bot.getConfig().get(event.getGuild());
		TextChannel suggestionChannel = config.getModerationConfig().getSuggestionChannel();
		if (event.getChannelType() != ChannelType.TEXT || !event.getChannel().asTextChannel().equals(suggestionChannel)) {
			Responses.warning(event, "This command can only be used in " + suggestionChannel.getAsMention()).queue();
			return;
		}
		OptionMapping messageIdMapping = event.getOption("message-id");
		if (messageIdMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		if (Checks.isInvalidLongInput(messageIdMapping)) {
			Responses.error(event, "Please provide a valid message id.").queue();
			return;
		}
		long messageId = messageIdMapping.getAsLong();
		event.deferReply(true).queue();
		event.getMessageChannel().retrieveMessageById(messageId).queue(
				message -> message.clearReactions().queue(s -> handleSuggestionCommand(event, message, config).queue()),
				e -> Responses.error(event.getHook(), "Could not find suggestion message with id " + messageId).queue());

	}

	protected abstract WebhookMessageCreateAction<Message> handleSuggestionCommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull Message message, GuildConfig config);

	protected ActionRow getJumpButton(@NotNull Message m) {
		return ActionRow.of(Button.link(m.getJumpUrl(), "Jump to Message"));
	}
}
