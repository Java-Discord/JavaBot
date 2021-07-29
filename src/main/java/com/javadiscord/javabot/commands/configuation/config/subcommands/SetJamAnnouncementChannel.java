package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetJamAnnouncementChannel implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        new Database().queryConfig(event.getGuild().getId(), "channels.jam_announcement_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Jam Announcement Channel", "Jam Announcement Channel successfully changed to", null, channel.getId(), true, true)).queue();

    }
}
