package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class ClearQOTW implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getOption("user").getAsMember();
        new Database().setMemberEntry(member.getId(), "qotwpoints", 0);
        return Responses.success(event, "Cleared QOTW-Points", "Successfully cleared all QOTW-Points from user " + member.getUser().getAsTag());
    }
}


