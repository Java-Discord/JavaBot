package net.javadiscord.javabot.systems.configuration;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.GuildConfig;
import net.javadiscord.javabot.data.config.UnknownPropertyException;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * An abstraction of {@link xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand} which handles all
 * config-related commands.
 */
public abstract class ConfigSubcommand extends SlashCommand.Subcommand {
	/**
	 * The main configuration of the bot.
	 */
	protected final BotConfig botConfig;

	public ConfigSubcommand(BotConfig botConfig) {
		this.botConfig = botConfig;
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null || event.getMember() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		if (!Checks.hasAdminRole(botConfig, event.getMember())) {
			Responses.replyAdminOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		try {
			handleConfigSubcommand(event, botConfig.get(event.getGuild())).queue();
		} catch (UnknownPropertyException e) {
			Responses.warning(event, "Unknown Property", "The provided property could not be found.")
					.queue();
		}
	}

	protected abstract ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) throws UnknownPropertyException;
}
