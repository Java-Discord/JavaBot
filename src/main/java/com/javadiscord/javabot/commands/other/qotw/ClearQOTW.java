package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Date;

public class ClearQOTW implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Member member = event.getOption("user").getAsMember();
        new Database().setMemberEntry(member.getId(), "qotwpoints", 0);
            var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | QOTW-Points cleared", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.RED)
                .setDescription("Succesfully cleared all QOTW-Points from " + member.getUser().getAsMention() + ".")
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();
        return event.replyEmbeds(e);
    }
}


