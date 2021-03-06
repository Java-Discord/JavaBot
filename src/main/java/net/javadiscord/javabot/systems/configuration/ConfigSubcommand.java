package net.javadiscord.javabot.systems.configuration;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Checks;
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
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasAdminRole(event.getGuild(), event.getMember())) {
			Responses.replyAdminOnly(event, event.getGuild()).queue();
			return;
		}
		try {
			handleConfigSubcommand(event, Bot.getConfig().get(event.getGuild())).queue();
		} catch (UnknownPropertyException e) {
			Responses.warning(event, "Unknown Property", "The provided property could not be found.")
					.queue();
		}
	}

	protected abstract ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException;
}
