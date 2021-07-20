package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetAvatarWidth implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        int width = (int) event.getOption("width").getAsLong();
        new Database().queryConfig(event.getGuild().getId(), "welcome_system.image.avatar.avW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image Width", "Avatar Image Width successfully changed to ", null, String.valueOf(width), true)).queue();
    }
}
