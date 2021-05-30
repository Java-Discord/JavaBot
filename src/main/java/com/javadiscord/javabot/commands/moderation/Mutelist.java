package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.Date;
import java.util.List;


public class Mutelist extends Command {

    public Mutelist () { this.name = "mutelist"; }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MANAGE_ROLES)) {

            String res = "";
            int memberSize;
            Role muteRole = Database.configRole(event, "mute_rid");

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

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor("Mutelist (" + memberSize + ")")
                    .setColor(new Color(0x2F3136))
                    .setDescription(res)
                    .setTimestamp(new Date().toInstant());
            event.reply(eb.build());

        } else {
            event.reply(Embeds.permissionError("MANAGE_ROLES", event));
        }
    }
}