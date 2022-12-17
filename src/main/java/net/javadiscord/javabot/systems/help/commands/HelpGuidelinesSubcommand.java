package net.javadiscord.javabot.systems.help.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringResourceCache;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The main configuration of the bot
	 */
	public HelpGuidelinesSubcommand(BotConfig botConfig) {
		setCommandData(new SubcommandData("guidelines", "Show the server's help guidelines in a simple format."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(StringResourceCache.load("/help_guidelines/guidelines_text.txt"))
				.setImage(StringResourceCache.load("/help_guidelines/guidelines_image_url.txt"))
				.build()
		).queue();
	}
}
