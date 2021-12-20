package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.util.List;

public class MutelistCommand implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        String res = "";
        int memberSize;
        Role muteRole = Bot.config.get(event.getGuild()).getModeration().getMuteRole();

        try {
            List<Member> members = event.getGuild().getMembersWithRoles(muteRole);
            StringBuilder sb = new StringBuilder();
            memberSize = members.size();
            for (Member member : members) {
                sb.append(member.getAsMention());
                sb.append("\n");
            }
            res = sb.toString();
        } catch (IllegalArgumentException e) {
            memberSize = 0;
        }
        return Responses.success(event, "Mutelist (" + memberSize + ")", res);
    }
}