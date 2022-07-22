package net.javadiscord.javabot.systems.tags.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/tag-admin` command. This holds administrative commands for managing "Custom Tags".
 */
public class CustomTagsAdminCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public CustomTagsAdminCommand() {
		setSlashCommandData(Commands.slash("tag-admin", "Administrative commands for managing \"Custom Tags\".")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new CreateCustomTagSubcommand(), new DeleteCustomTagSubcommand(), new EditCustomTagSubcommand());
	}
}