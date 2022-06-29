package net.javadiscord.javabot.systems.jam;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.systems.jam.subcommands.admin.*;

/**
 * Represents the `/jam-admin` command. This holds administrative commands for configuring the Java Jam.
 */
public class JamAdminCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public JamAdminCommand() {
		setSlashCommandData(Commands.slash("jam-admin", "Administrator actions for configuring the Java Jam.")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
		addSubcommands(new PlanNewJamSubcommand(), new EditJamSubcommand(), new NextPhaseSubcommand(),
				new AddThemeSubcommand(), new ListThemesSubcommand(), new RemoveThemeSubcommand(),
				new ListSubmissionsSubcommand(), new RemoveSubmissionsSubcommand(), new CancelSubcommand());
	}
}
