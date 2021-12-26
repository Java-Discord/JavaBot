package net.javadiscord.javabot.systems.jam;

import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.systems.jam.subcommands.admin.*;

import java.util.Map;

public class JamAdminCommandHandler extends DelegatingCommandHandler {
	public JamAdminCommandHandler() {
		super(Map.of(
				"plan-new-jam", new PlanNewJamSubcommand(),
				"edit-jam", new EditJamSubcommand(),
				"add-theme", new AddThemeSubcommand(),
				"list-themes", new ListThemesSubcommand(),
				"remove-theme", new RemoveThemeSubcommand(),
				"next-phase", new NextPhaseSubcommand(),
				"list-submissions", new ListSubmissionsSubcommand(),
				"remove-submissions", new RemoveSubmissionsSubcommand(),
				"cancel", new CancelSubcommand()
		));
	}
}
