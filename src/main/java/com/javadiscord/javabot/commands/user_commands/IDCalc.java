package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class IDCalc implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        long id;
        try {
            id = Objects.requireNonNull(event.getOption("id")).getAsLong();
        } catch (Exception e) {
            id = event.getUser().getIdLong();
        }
        long unixTimeStampMilliseconds = id / 4194304 + 1420070400000L;
        long unixTimeStamp = unixTimeStampMilliseconds / 1000;

        String date = Instant.ofEpochMilli(unixTimeStampMilliseconds).atZone(ZoneId.of("GMT")).format(TimeUtils.STANDARD_FORMATTER);

        EmbedBuilder eb = new EmbedBuilder()
            .setAuthor("ID-Calculator")
            .setColor(Constants.GRAY)
            .addField("ID", "```" + id + "```", false)
            .addField("Unix-Timestamp (+ milliseconds)", "```" + unixTimeStampMilliseconds + "```", false)
            .addField("Unix-Timestamp", "```" + unixTimeStamp + "```", false)
            .addField("date", "```" + date + "```", false);

        return event.replyEmbeds(eb.build());
    }
}