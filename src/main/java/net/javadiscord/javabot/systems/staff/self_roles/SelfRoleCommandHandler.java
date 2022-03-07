package net.javadiscord.javabot.systems.staff.self_roles;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.self_roles.subcommands.CreateSelfRoleSubcommand;
import net.javadiscord.javabot.systems.staff.self_roles.subcommands.DisableSelfRoleSubcommand;
import net.javadiscord.javabot.systems.staff.self_roles.subcommands.EnableSelfRoleSubcommand;

/**
 * Handler class for all Reaction Role related commands.
 */
public class SelfRoleCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler}.
	 */
	public SelfRoleCommandHandler() {
		this.addSubcommand("create", new CreateSelfRoleSubcommand());
		this.addSubcommand("enable", new EnableSelfRoleSubcommand());
		this.addSubcommand("disable", new DisableSelfRoleSubcommand());
	}

	@Override
	public InteractionCallbackAction<InteractionHook> handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			return super.handleSlashCommandInteraction(event);
		} catch(ResponseException e) {
			return Responses.error(event, String.format("```%s```", e.getMessage()));
		}
	}
}

