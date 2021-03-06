package net.javadiscord.javabot.systems.staff_commands.suggestions;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.moderation.CommandModerationPermissions;

/**
 * Represents the `/suggestion` command. This holds administrative commands for managing server suggestions.
 */
public class SuggestionCommand extends SlashCommand implements CommandModerationPermissions {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public SuggestionCommand() {
		setModerationSlashCommandData(Commands.slash("suggestion", "Administrative commands for managing suggestions.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new AcceptSuggestionSubcommand(), new DeclineSuggestionSubcommand(), new ClearSuggestionSubcommand(), new OnHoldSuggestionSubcommand());
	}
}
