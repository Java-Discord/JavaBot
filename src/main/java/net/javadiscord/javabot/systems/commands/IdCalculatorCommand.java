package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.util.TimeUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public class IdCalculatorCommand implements SlashCommandHandler {
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
            .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
            .addField("ID", "```" + id + "```", false)
            .addField("Unix-Timestamp (+ milliseconds)", "```" + unixTimeStampMilliseconds + "```", false)
            .addField("Unix-Timestamp", "```" + unixTimeStamp + "```", false)
            .addField("date", "```" + date + "```", false);

        return event.replyEmbeds(eb.build());
    }
}