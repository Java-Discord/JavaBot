package com.javadiscord.javabot.commands.jam;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.jam.subcommands.JamInfoSubcommand;
import com.javadiscord.javabot.commands.jam.subcommands.JamSubmitSubcommand;
import com.javadiscord.javabot.commands.jam.subcommands.admin.*;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Main command handler for Jam commands. This handler is responsible for
 * delegating command handling to subcommand handlers.
 */
public class JamCommandHandler implements SlashCommandHandler {
	private final Map<String, SlashCommandHandler> subcommandHandlers;

	public JamCommandHandler() {
		this.subcommandHandlers = new HashMap<>();
		// General purpose subcommands.
		this.subcommandHandlers.put("info", new JamInfoSubcommand());
		this.subcommandHandlers.put("submit", new JamSubmitSubcommand());

		// Admin subcommands.
		this.subcommandHandlers.put("plan-new-jam", new PlanNewJamSubcommand());
		this.subcommandHandlers.put("add-theme", new AddThemeSubcommand());
		this.subcommandHandlers.put("list-themes", new ListThemesSubcommand());
		this.subcommandHandlers.put("remove-theme", new RemoveThemeSubcommand());
		this.subcommandHandlers.put("next-phase", new NextPhaseSubcommand());
		this.subcommandHandlers.put("list-submissions", new ListSubmissionsSubcommand());
		this.subcommandHandlers.put("remove-submissions", new RemoveSubmissionsSubcommand());
		this.subcommandHandlers.put("cancel", new CancelSubcommand());
	}

	@Override
	public void handle(SlashCommandEvent event) {
		if (event.getSubcommandName() != null) {
			SlashCommandHandler handler = this.subcommandHandlers.get(event.getSubcommandName());
			if (handler != null) {
				handler.handle(event);
			} else {
				event.getHook().setEphemeral(true);
				event.getHook().sendMessage("Unknown subcommand.").queue();
			}
		}
	}

	/**
	 * Helper method that checks if the user who sent a slash command is a Jam
	 * admin, and if not, sends a standard "no permission" message.
	 * <p>
	 *     Generally, if this method returns false, admin command processing can
	 *     simply stop, and the handler can be sure that the client has been
	 *     notified of the lack of permission.
	 * </p>
	 * @param event The event that the user sent.
	 * @return True if the user is a Jam admin, or false otherwise.
	 */
	public static boolean ensureAdmin(SlashCommandEvent event) {

		Role adminRole = new Database().getConfigRole(event.getGuild(), "roles.jam_admin_rid");

		if (event.getMember() == null || !event.getMember().getRoles().contains(adminRole)) {
			event.getHook().setEphemeral(true);
			event.getHook().sendMessage("You don't have permission to use this command.").queue();
			return false;
		}
		return true;
	}
}
