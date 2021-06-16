package com.javadiscord.javabot.commands.other.qotw;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Date;

public class ClearQOTW extends Command {

    public ClearQOTW () {
        this.name = "clearqotw";
        this.category = new Category("MODERATION");
        this.arguments = "<@User/ID>";
        this.help = "clears all qotw-points from the given user";
    }

    protected void execute(CommandEvent event) {
            if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                String[] args = event.getArgs().split("\\s+");
                Member member = null;
                String Reason;

                try {
                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);
                        } else {
                            member = event.getGuild().getMemberById(args[0]);
                        }
                    }

                    String Tag = member.getUser().getAsTag();
                    String AvatarURL = member.getUser().getEffectiveAvatarUrl();
                    String ID = member.getId();

                    Database.queryMemberInt(ID, "qotwpoints", 0);

                    EmbedBuilder eb = new EmbedBuilder()
                            .setAuthor(Tag + " | QOTW-Points cleared", null, AvatarURL)
                            .setColor(Constants.RED)
                            .setDescription("Succesfully cleared all QOTW-Points from " + member.getUser().getAsMention() + ".")
                            .setFooter("ID: " + ID)
                            .setTimestamp(new Date().toInstant());
                    event.reply(eb.build());

                } catch (NullPointerException | IllegalArgumentException e) {
                    event.reply(Embeds.syntaxError("clearqotw @User/ID", event));
                }

                } else {
                    event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
                }
        }
    }


