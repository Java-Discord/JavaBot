package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetBackgroundUrl implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String url = event.getOption("url").getAsString();
        if (Misc.isImage(url)) return event.replyEmbeds(new Config().configEmbed(
                    "Welcome Image Background URL",
                    "`" + url + "`"
            ));
            //new Database().queryConfig(event.getGuild().getId(), "welcome_system.image.bgURL", url);
        else return Responses.error(event, "```URL must be a valid HTTP(S) or Attachment URL.```");
    }
}
