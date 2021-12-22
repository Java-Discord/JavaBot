package net.javadiscord.javabot.systems.qotw;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.mongodb.Database;

// TODO: Merge with /qotw command
public class ClearQOTWCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getOption("user").getAsMember();
        new Database().setMemberEntry(member.getId(), "qotwpoints", 0);
        return Responses.success(event, "Cleared QOTW-Points", "Successfully cleared all QOTW-Points from user " + member.getUser().getAsTag());
    }
}


