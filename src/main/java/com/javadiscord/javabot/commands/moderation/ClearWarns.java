package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Date;

public class ClearWarns extends Command {

    public ClearWarns () {

        this.name = "clearwarns";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                Member member = null;
                String[] args = event.getArgs().split("\\s+");

                try {

                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);

                        } else {
                            member = event.getGuild().getMemberById(args[0]);
                        }
                    }

                    Database.queryMemberInt(member.getId(), "warns", 0);

                    EmbedBuilder eb = new EmbedBuilder()
                            .setAuthor(member.getUser().getAsTag() + " | Warns cleared", null, member.getUser().getEffectiveAvatarUrl())
                            .setColor(Constants.YELLOW)
                            .setDescription("Succesfully cleared all warns from " + member.getUser().getAsMention() + ".")
                            .setFooter("ID: " + member.getId())
                            .setTimestamp(new Date().toInstant());
                    event.reply(eb.build());

                } catch (NullPointerException | IllegalArgumentException e) { event.reply(Embeds.syntaxError("clearwarns @User/ID", event)); }
                } else { event.reply(Embeds.permissionError("MESSAGE_MANAGE", event)); }
        }
    }

