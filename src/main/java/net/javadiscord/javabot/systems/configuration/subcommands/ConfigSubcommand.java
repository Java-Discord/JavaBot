package net.javadiscord.javabot.systems.configuration.subcommands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * An abstraction of {@link com.dynxsty.dih4jda.interactions.commands.SlashCommand.Subcommand} which handles all
 * config-related commands.
 */
public abstract class ConfigSubcommand extends SlashCommand.Subcommand {
	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!event.isFromGuild()) {
			Responses.error(event, "This command may only be used inside servers.").queue();
		}
		try {
			handleConfigSubcommand(event, Bot.config.get(event.getGuild())).queue();
		} catch (UnknownPropertyException e) {
			Responses.warning(event, "Unknown Property", "The provided property could not be found.")
					.queue();
		}
	}

	protected abstract ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException;
}
