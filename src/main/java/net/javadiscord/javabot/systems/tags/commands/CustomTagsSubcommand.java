package net.javadiscord.javabot.systems.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * An abstraction of {@link com.dynxsty.dih4jda.interactions.commands.SlashCommand.Subcommand} which handles all
 * custom-tag-related commands.
 */
@Slf4j
public abstract class CustomTagsSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.guildOnly(event).queue();
			return;
		}
		try {
			handleCustomTagsSubcommand(event).queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, "An error occurred while executing this command.");
		}
	}

	protected abstract InteractionCallbackAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) throws SQLException;
}
