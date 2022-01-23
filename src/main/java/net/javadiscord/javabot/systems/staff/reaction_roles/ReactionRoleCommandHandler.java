package net.javadiscord.javabot.systems.staff.reaction_roles;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.DelegatingCommandHandler;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.systems.staff.reaction_roles.subcommands.CreateSubcommand;
import net.javadiscord.javabot.systems.staff.reaction_roles.subcommands.DeleteSubcommand;

public class ReactionRoleCommandHandler extends DelegatingCommandHandler {

	public ReactionRoleCommandHandler() {
		addSubcommand("create", new CreateSubcommand());
		addSubcommand("delete", new DeleteSubcommand());
	}

	@Override
	public ReplyAction handle(SlashCommandEvent event) {

		try {
			return super.handle(event);
		} catch (Exception e) {
			return Responses.error(event, "```" + e.getMessage() + "```");
		}
	}
}

