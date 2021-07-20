package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetAvatarY implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        int y = (int) event.getOption("y").getAsLong();
        new Database().queryConfig(event.getGuild().getId(), "welcome_system.image.avatar.avY", y);
        event.replyEmbeds(Embeds.configEmbed(event, "Avatar Image (Y-Pos)", "Avatar Image ``(Y-Position)`` successfully changed to ", null, String.valueOf(y), true)).queue();

    }
}
