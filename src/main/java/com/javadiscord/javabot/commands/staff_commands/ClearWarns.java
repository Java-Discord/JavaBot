package com.javadiscord.javabot.commands.staff_commands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.data.mongodb.Database;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class ClearWarns implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getOption("user").getAsMember();
        new Database().setMemberEntry(member.getId(), "warns", 0);
        new Warn().deleteAllDocs(member.getId());
        return Responses.success(event, "Warns cleared", "Successfully cleared all warns from user " + member.getUser().getAsMention());
    }
}

