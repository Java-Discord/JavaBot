package net.javadiscord.javabot.systems.staff.reaction_roles;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.reaction_roles.subcommands.CreateSubcommand;
import net.javadiscord.javabot.systems.staff.reaction_roles.subcommands.DeleteSubcommand;

/**
 * Handler class for all Reaction Role related commands.
 */
public class ReactionRoleCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler}.
	 */
	public ReactionRoleCommandHandler() {
		addSubcommand("create", new CreateSubcommand());
		addSubcommand("delete", new DeleteSubcommand());
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			return super.handleSlashCommandInteraction(event);
		} catch (ResponseException e) {
			return Responses.error(event, String.format("```%s```", e.getMessage()));
		}
	}
}

