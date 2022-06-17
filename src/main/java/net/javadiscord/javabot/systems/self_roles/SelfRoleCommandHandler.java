package net.javadiscord.javabot.systems.self_roles;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.systems.self_roles.subcommands.DisableSelfRoleSubcommand;
import net.javadiscord.javabot.systems.self_roles.subcommands.EnableSelfRoleSubcommand;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.systems.self_roles.subcommands.CreateSelfRoleSubcommand;

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
	public InteractionCallbackAction<?> handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			return super.handleSlashCommandInteraction(event);
		} catch(ResponseException e) {
			return Responses.error(event, String.format("```%s```", e.getMessage()));
		}
	}
}

