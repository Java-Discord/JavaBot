package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetAvatarHeight implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        int height = (int) event.getOption("height").getAsLong();
        //new Database().queryConfig(event.getGuild().getId(), "welcome_system.image.avatar.avH", height);
        return event.replyEmbeds(new Config().configEmbed(
                "Avatar Image Height",
                "`" + height + "`"
        ));
    }
}
