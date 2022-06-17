package net.javadiscord.javabot.systems.staff.suggestions;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.AcceptSuggestionSubcommand;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.ClearSuggestionSubcommand;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.DeclineSuggestionSubcommand;
import net.javadiscord.javabot.systems.staff.suggestions.subcommands.OnHoldSuggestionSubcommand;

/**
 * Represents the `/suggestion` command. This holds administrative commands for managing server suggestions.
 */
public class SuggestionCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public SuggestionCommand() {
		setSlashCommandData(Commands.slash("suggestion", "Administrative commands for managing suggestions.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new AcceptSuggestionSubcommand(), new DeclineSuggestionSubcommand(), new ClearSuggestionSubcommand());
	}
}
