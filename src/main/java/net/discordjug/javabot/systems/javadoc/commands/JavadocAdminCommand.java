package net.discordjug.javabot.systems.javadoc.commands;

import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;

/**
 * Subcommand that allows staff-members to add question to the QOTW-Queue.
 */
public class JavadocAdminCommand extends SlashCommand{
	/**
	 * This classes constructor which sets the {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} and
	 * adds the corresponding {@link net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup}s.
	 *
	 * @param downloadSubcommand   /docs-admin download
	 * @param deleteSubcommand   /docs-admin delete
	 */
	public JavadocAdminCommand(JavadocDownloadSubcommand downloadSubcommand, JavadocDeleteSubcommand deleteSubcommand) {
		setCommandData(Commands.slash("docs-admin", "Administrative tools for managing the Javadoc functionality.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(downloadSubcommand, deleteSubcommand);
	}
}
