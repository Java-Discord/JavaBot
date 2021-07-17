package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetPrimaryColor implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String color = event.getOption("color").getAsString();
        color = color.replace("#", "");
        long l = Long.parseLong(color, 16);
        int i = (int) l;

        Database.queryConfig(event.getGuild().getId(), "welcome_system.image.primCol", i);
        event.replyEmbeds(Embeds.configEmbed(event, "Primary Welcome Image Color", "Primary Welcome Image Color successfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true)).queue();
    }
}
