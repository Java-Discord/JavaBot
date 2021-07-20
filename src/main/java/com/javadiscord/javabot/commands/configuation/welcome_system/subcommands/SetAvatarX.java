package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetAvatarX implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        int x = (int) event.getOption("x").getAsLong();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.image.avatar.avX", x);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (X-Pos)", "Avatar Image ``(X-Position)`` successfully changed to ", null, String.valueOf(x), true)).queue();
    }
}