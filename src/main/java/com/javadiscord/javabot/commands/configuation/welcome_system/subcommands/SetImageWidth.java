package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetImageWidth implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        int width = (int) event.getOption("width").getAsLong();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.image.imgW", width);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Width", "Welcome Image Width successfully changed to ", null, String.valueOf(width), true)).queue();
    }
}
