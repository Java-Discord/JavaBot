package net.discordjug.javabot.systems.javadoc.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

public class JavadocDeleteSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public JavadocDeleteSubcommand() {
		setCommandData(new SubcommandData("delete", "Deletes an existing Javadoc package."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent slashCommandInteractionEvent) {

	}
}
