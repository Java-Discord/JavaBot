package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.data.mongodb.Database;

public class ClearWarnsCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getOption("user").getAsMember();
        new Database().setMemberEntry(member.getId(), "warns", 0);
        new WarnCommand().deleteAllDocs(member.getId());
        return Responses.success(event, "Warns cleared", "Successfully cleared all warns from user " + member.getUser().getAsMention());
    }
}

