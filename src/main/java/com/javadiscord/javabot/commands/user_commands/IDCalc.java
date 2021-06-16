package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;

public class IDCalc {

    public static void execute(SlashCommandEvent event, long id) {

        long unixTimeStampMilliseconds = id / 4194304 + 1420070400000L;
        long unixTimeStamp = unixTimeStampMilliseconds / 1000;

        String date = Instant.ofEpochMilli(unixTimeStampMilliseconds).atZone(ZoneId.of("GMT")).format(TimeUtils.STANDARD_FORMATTER);

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor("ID-Calculator")
                .setColor(new Color(0x2F3136))
                .addField("ID", "```" + id + "```", false)
                .addField("Unix-Timestamp (+ milliseconds)", "```" + unixTimeStampMilliseconds + "```", false)
                .addField("Unix-Timestamp", "```" + unixTimeStamp + "```", false)
                .addField("date", "```" + date + "```", false);

        event.replyEmbeds(eb.build()).queue();
    }
}