package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.Date;

public class ServerInfo implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        if (event.getGuild() == null) return Responses.warning(event, "This can only be used in a guild.");
        long roleCount = (long) event.getGuild().getRoles().size() - 1;
        long catCount = event.getGuild().getCategories().size();
        long textChannelCount = event.getGuild().getTextChannels().size();
        long voiceChannelCount = event.getGuild().getVoiceChannels().size();
        long channelCount = (long) event.getGuild().getChannels().size() - catCount;

        String guildDate = event.getGuild().getTimeCreated().format(TimeUtils.STANDARD_FORMATTER);
        String createdDiff = " (" + new TimeUtils().formatDurationToNow(event.getGuild().getTimeCreated()) + ")";

        EmbedBuilder eb = new EmbedBuilder()
            .setColor(Color.decode(
                    Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor()))
            .setThumbnail(event.getGuild().getIconUrl())
            .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
            .addField("Name", "```" + event.getGuild().getName() + "```", true)
            .addField("Owner", "```" + event.getGuild().getOwner().getUser().getAsTag() + "```", true)
            .addField("ID", "```" + event.getGuild().getId() + "```", false)
            .addField("Roles", "```" + roleCount + " Roles```", true)
            .addField("Channel Count", "```" + channelCount + " Channel, " + catCount + " Categories" +
                "\n → Text: " + textChannelCount +
                "\n → Voice: " + voiceChannelCount + "```", false)

            .addField("Member Count", "```" + event.getGuild().getMemberCount() + " members```", false)
            .addField("Server created on", "```" + guildDate + createdDiff + "```", false)
            .setTimestamp(new Date().toInstant());

        if (event.getGuild().getId().equals("648956210850299986")) {
            return event.replyEmbeds(eb.build()).addActionRow(Button.link(Constants.WEBSITE_LINK, "Website"));
        } else {
            return event.replyEmbeds(eb.build());
        }
    }
}
