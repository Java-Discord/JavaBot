package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
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
        return event.replyEmbeds(Embeds.configEmbed(event, "Secondary Welcome Image Color", "Secondary Welcome Image Color successfully changed to ", null, i + " (#" + Integer.toHexString(i) + ")", true));
    }
}
