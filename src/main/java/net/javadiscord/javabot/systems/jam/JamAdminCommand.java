package net.javadiscord.javabot.systems.jam;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.CommandPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.jam.subcommands.admin.*;

/**
 * Handler class for all jam-admin commands.
 */
public class JamAdminCommand extends SlashCommand {

	public JamAdminCommand() {
		setSlashCommandData(Commands.slash("jam-admin", "Administrator actions for configuring the Java Jam.")
				.setDefaultPermissions(CommandPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new PlanNewJamSubcommand(), new EditJamSubcommand(), new NextPhaseSubcommand(),
				new AddThemeSubcommand(), new ListThemesSubcommand(), new RemoveThemeSubcommand(),
				new ListSubmissionsSubcommand(), new RemoveSubmissionsSubcommand(), new CancelSubcommand());
	}
}
