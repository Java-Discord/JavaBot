package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetStarboardChannel implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        new Database().queryConfig(event.getGuild().getId(), "other.starboard.starboard_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Starboard Channel", "Starboard Channel succesfully changed to", null, channel.getId(), true, true)).queue();

    }
}
