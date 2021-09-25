package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import java.util.Date;

public class Correct {

    public void correct(ButtonClickEvent event, Member member) {

        int qotwPoints = new Database().getMemberInt(member, "qotwpoints") + 1;
        new Database().setMemberEntry(member.getId(), "qotwpoints", qotwPoints);

        var eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | QOTW-Point added", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.GREEN)
                .addField("Total QOTW-Points", "```" + qotwPoints + "```", true)
                .addField("Rank", "```#" + new Leaderboard().getQOTWRank(event.getGuild(), member.getId()) + "```", true)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();
        Misc.sendToLog(event.getGuild(), eb);

        if (!member.getUser().hasPrivateChannel()) Misc.sendToLog(event.getGuild(), "> Couldn't send Message to User " + member.getUser().getAsTag());
        member.getUser().openPrivateChannel().complete()
                .sendMessageEmbeds(new EmbedBuilder()
                        .setAuthor("Question of the Week", null, member.getUser().getEffectiveAvatarUrl())
                        .setColor(Constants.GREEN)
                        .setDescription("Your answer was correct! " + Constants.SUCCESS +
                                "\nYou've been granted **1 QOTW-Point!** (Total: " + qotwPoints + ")")
                        .setTimestamp(new Date().toInstant())
                        .build())
                .queue();
    }
}
