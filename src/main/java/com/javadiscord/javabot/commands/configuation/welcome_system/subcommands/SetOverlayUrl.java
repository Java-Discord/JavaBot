package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetOverlayUrl implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String url = event.getOption("url").getAsString();
        if (Misc.isImage(url)) {
            Database.queryConfig(event.getGuild().getId(), "welcome_system.image.overlayURL", url);
            return event.replyEmbeds(Embeds.configEmbed(event, "Welcome Image Overlay", "Welcome Image Overlay successfully changed to ", Misc.checkImage(url), url, true));
        } else {
            return event.replyEmbeds(Embeds.emptyError("```URL must be a valid HTTP(S) or Attachment URL.```", event.getUser()));
        }
    }
}
