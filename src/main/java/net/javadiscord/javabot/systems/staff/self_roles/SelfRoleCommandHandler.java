package net.javadiscord.javabot.systems.staff.self_roles;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.self_roles.subcommands.CreateSelfRoleSubcommand;

/**
 * Handler class for all Reaction Role related commands.
 */
public class SelfRoleCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler}.
	 */
	public SelfRoleCommandHandler() {
		addSubcommand("create", new CreateSelfRoleSubcommand());
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

