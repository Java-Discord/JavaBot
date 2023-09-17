package net.javadiscord.javabot.systems.configuration;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.data.config.BotConfig;
import net.dv8tion.jda.api.utils.FileUpload;
import net.javadiscord.javabot.data.config.GuildConfig;
import javax.annotation.Nonnull;
import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ExportConfigSubcommand extends ConfigSubcommand {

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public ExportConfigSubcommand(BotConfig botConfig) {
		super(botConfig);
		setCommandData(new SubcommandData("export", "Exports a list of all configuration properties, and their current values."));
	}

	@Override
	public ReplyCallbackAction handleConfigSubcommand(@Nonnull SlashCommandInteractionEvent event, @Nonnull GuildConfig config) {
		return event.deferReply()
				.addFiles(FileUpload.fromData(new File("config/" + event.getGuild().getId() + ".json")));
	}
}
