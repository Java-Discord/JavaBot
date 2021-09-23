package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetSecondaryColor implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String color = event.getOption("color").getAsString();
        color = color.replace("#", "");
        long l = Long.parseLong(color, 16);
        int i = (int) l;

        //new Database().queryConfig(event.getGuild().getId(), "welcome_system.image.secCol", i);
        return event.replyEmbeds(new Config().configEmbed(
                "Secondary Color",
                "`#" + Integer.toHexString(i) + "`"
        ));
    }
}
