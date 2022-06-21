package net.javadiscord.javabot.systems.configuration.subcommands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.GuildConfig;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ListConfigSubcommand extends ConfigSubcommand {
	public ListConfigSubcommand() {
		setSubcommandData(new SubcommandData("list", "Get a list of all configuration properties, and their current values."));
	}

	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) {
		return event.deferReply()
				.addFile(new File("config/" + event.getGuild().getId() + ".json"));
	}
}