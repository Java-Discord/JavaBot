package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetAvatarWidth implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        int width = (int) event.getOption("width").getAsLong();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.image.avatar.avW", width);
        return event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Width", "Avatar Image Width successfully changed to ", null, String.valueOf(width), true));
    }
}
