package net.javadiscord.javabot.systems.custom_commands.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.systems.custom_commands.CustomCommandManager;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Slf4j
public abstract class CustomCommandsSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		if (!event.isFromGuild() || event.getGuild() == null) {
			Responses.error(event, "This command may only be used inside servers.").queue();
			return;
		}
		try {
			handleCustomCommandsSubcommand(event, CustomCommandManager.cleanString(nameMapping.getAsString())).queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, "An error occurred while executing this command.");
		}
	}

	protected abstract ReplyCallbackAction handleCustomCommandsSubcommand(@NotNull SlashCommandInteractionEvent event, @NotNull String commandName) throws SQLException;
}
