package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetJamAdminRole implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Role role = event.getOption("role").getAsRole();
        //new Database().queryConfig(event.getGuild().getId(), "roles.jam_admin_rid", role.getId());
        return event.replyEmbeds(new Config().configEmbed(
                "Jam Admin Role",
                role.getAsMention()
        ));
    }
}
