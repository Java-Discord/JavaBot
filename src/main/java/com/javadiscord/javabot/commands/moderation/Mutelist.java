package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class Mutelist implements SlashCommandHandler {

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

        var e = new EmbedBuilder()
                .setAuthor("Mutelist (" + memberSize + ")")
                .setColor(Color.decode(
                        Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor()))
                .setDescription(res)
                .setTimestamp(new Date().toInstant())
                .build();

        return event.replyEmbeds(e);
    }
}