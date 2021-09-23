package com.javadiscord.javabot.commands.reaction_roles;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.reaction_roles.subcommands.CreateReactionRole;
import com.javadiscord.javabot.commands.reaction_roles.subcommands.DeleteReactionRole;
import com.javadiscord.javabot.commands.reaction_roles.subcommands.ListReactionRoles;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class ReactionRoles extends DelegatingCommandHandler {

    public ReactionRoles() {
        addSubcommand("create", new CreateReactionRole());
        addSubcommand("delete", new DeleteReactionRole());
        addSubcommand("list", new ListReactionRoles());
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        try { return super.handle(event);
        } catch (Exception e) { return Responses.error(event, "```" + e.getMessage() + "```"); }
    }
}

