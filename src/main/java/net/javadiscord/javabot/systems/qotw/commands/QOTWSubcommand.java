package net.javadiscord.javabot.systems.qotw.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;

/**
 * Abstract parent class for all QOTW subcommands, which handles the standard
 * behavior of preparing a connection and obtaining the guild id; these two
 * things are required for all QOTW subcommands.
 */
public abstract class QOTWSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		try {
			InteractionCallbackAction<?> reply = handleCommand(event, event.getGuild().getIdLong());
			reply.queue();
		} catch (DataAccessException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, "An error occurred: " + e.getMessage()).queue();
		}
	}

	protected abstract InteractionCallbackAction<?> handleCommand(SlashCommandInteractionEvent event, long guildId) throws DataAccessException;
}
