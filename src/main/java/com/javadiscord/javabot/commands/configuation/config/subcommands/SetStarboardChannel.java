package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetStarboardChannel implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        //new Database().queryConfig(event.getGuild().getId(), "other.starboard.starboard_cid", channel.getId());
        return event.replyEmbeds(new Config().configEmbed(
                "Starboard Channel",
                "<#" + channel.getId() + ">"
        ));
    }
}
