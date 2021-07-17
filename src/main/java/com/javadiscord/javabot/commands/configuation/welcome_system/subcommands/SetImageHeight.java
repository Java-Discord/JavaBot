package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetImageHeight implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        int height = (int) event.getOption("height").getAsLong();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.image.imgH", height);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Height", "Welcome Image Height successfully changed to ", null, String.valueOf(height), true)).queue();
    }
}
