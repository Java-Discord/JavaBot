package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetBackgroundUrl implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String url = event.getOption("url").getAsString();
        if (Misc.isImage(url)) {
            Database.queryConfig(event.getGuild().getId(), "welcome_system.image.bgURL", url);
            event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Background", "Welcome Image Background successfully changed to ", Misc.checkImage(url), url, true)).queue();
        } else {
            event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event.getUser())).queue();
        }
    }
}
