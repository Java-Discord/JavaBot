package net.discordjug.javabot.systems.staff_commands.tags.commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

import lombok.RequiredArgsConstructor;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.ExceptionLogger;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * An abstraction of {@link xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand} which handles all
 * custom-tag-related commands.
 */
@RequiredArgsConstructor
public abstract class TagsSubcommand extends SlashCommand.Subcommand {
	private boolean requireStaff = true;
	private final BotConfig botConfig;

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (requireStaff && !Checks.hasStaffRole(botConfig, event.getMember())) {
			Responses.replyStaffOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		try {
			handleCustomTagsSubcommand(event).queue();
		} catch (SQLException e) {
			ExceptionLogger.capture(e, getClass().getSimpleName());
			Responses.error(event, "An error occurred while executing this command.");
		}
	}

	protected void setRequiredStaff(boolean requireStaff) {
		this.requireStaff = requireStaff;
	}

	protected abstract InteractionCallbackAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) throws SQLException;
}
