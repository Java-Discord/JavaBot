package net.javadiscord.javabot.systems.jam;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.interfaces.Autocompletable;
import net.javadiscord.javabot.systems.jam.subcommands.admin.*;

import java.util.Map;

/**
 * Handler class for all jam-admin commands.
 */
public class JamAdminCommandHandler extends DelegatingCommandHandler implements Autocompletable {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
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

	@Override
	public AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
		return switch (event.getSubcommandName()) {
			case "remove-submissions" -> ListSubmissionsSubcommand.replySubmissions(event);
			case "remove-theme" -> ListThemesSubcommand.replyThemes(event);
			default -> event.replyChoices();
		};
	}
}
