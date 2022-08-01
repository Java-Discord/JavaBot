package net.javadiscord.javabot.systems.staff_commands.self_roles;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Represents the `/self-role` command. This holds administrative commands for managing the bot's database.
 */
public class SelfRoleCommand extends SlashCommand {
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.Subcommand}s.
	 */
	public SelfRoleCommand() {
		setSlashCommandData(Commands.slash("self-role", "Administrative Commands for managing Self Roles.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new CreateSelfRoleSubcommand(), new ChangeSelfRoleStatusSubcommand(), new RemoveSelfRolesSubcommand());
	}
}

