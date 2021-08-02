package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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
        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            return event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }

        String res = "";
        int memberSize;
        Role muteRole = new Database().getConfigRole(event.getGuild(), "roles.mute_rid");

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
                .setColor(new Color(0x2F3136))
                .setDescription(res)
                .setTimestamp(new Date().toInstant())
                .build();

        return event.replyEmbeds(e);
    }
}