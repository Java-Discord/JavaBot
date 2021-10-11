package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.Date;

public class Correct implements SlashCommandHandler {

    public void correct(Guild guild, Member member) {

        int qotwPoints = new Database().getMemberInt(member, "qotwpoints") + 1;
        new Database().setMemberEntry(member.getId(), "qotwpoints", qotwPoints);

        var eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
                .addField("Total QOTW-Points", "```" + qotwPoints + "```", true)
                .addField("Rank", "```#" + new Leaderboard().getQOTWRank(guild, member.getId()) + "```", true)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();
        Misc.sendToLog(guild, eb);

        if (!member.getUser().hasPrivateChannel()) {
            Misc.sendToLog(guild, "> Couldn't send Message to User " + member.getUser().getAsTag());
            member.getUser().openPrivateChannel().complete()
                    .sendMessageEmbeds(new EmbedBuilder()
                            .setAuthor("Question of the Week", null, member.getUser().getEffectiveAvatarUrl())
                            .setColor(Bot.config.get(guild).getSlashCommand().getSuccessColor())
                            .setDescription("Your answer was correct! " + Bot.config.get(guild).getEmote().getSuccessEmote() +
                                    "\nYou've been granted **1 QOTW-Point!** (Total: " + qotwPoints + ")")
                            .setTimestamp(new Date().toInstant())
                            .build())
                    .queue();
        }
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member mem = event.getOption("member").getAsMember();
        correct(event.getGuild(), mem);
        return Responses.success(event, "Correct",
                "Successfully granted Member **" + mem.getUser().getAsTag() + "** one QOTW-Point!");
    }

}
