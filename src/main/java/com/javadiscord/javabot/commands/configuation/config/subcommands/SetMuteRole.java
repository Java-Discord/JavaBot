package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetMuteRole implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Role role = event.getOption("role").getAsRole();
        new Database().queryConfig(event.getGuild().getId(), "roles.mute_rid", role.getId());
        return event.replyEmbeds(Embeds.configEmbed(event, "Mute Role", "Mute Role successfully changed to", null, role.getId(), true, false, true));
    }
}
