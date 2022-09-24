package net.javadiscord.javabot.systems.configuration;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.GuildConfig;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ExportConfigSubcommand extends ConfigSubcommand {
	public ExportConfigSubcommand() {
		setSubcommandData(new SubcommandData("export", "Exports a list of all configuration properties, and their current values."));
		requireUsers(Bot.getConfig().getSystems().getAdminConfig().getAdminUsers());
	}

	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) {
		return event.deferReply()
				.addFiles(FileUpload.fromData(new File("config/" + event.getGuild().getId() + ".json")));
	}
}
