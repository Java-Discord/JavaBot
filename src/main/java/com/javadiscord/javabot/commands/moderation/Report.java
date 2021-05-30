package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class Report extends Command {

    public Report () { this.name = "report"; }

    protected void execute(CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");
        Member member = null;
        String reason = null;



            

                if (event.getMessage().getReferencedMessage() == null) {

                    try {

                    if (args.length >= 1) {
                        if (!event.getMessage().getMentionedMembers().isEmpty()) {
                            member = event.getMessage().getMentionedMembers().get(0);

                        } else {
                            member = event.getGuild().getMemberById(args[0]);
                        }

                        if (event.getMessage().getMember().equals(member)) {
                            event.reply(Embeds.selfPunishError("report", event));
                            return;
                        }
                    }

                    if (args.length >= 2) {
                        String[] reasonArray = Arrays.copyOfRange(args, 1, args.length);

                        StringBuilder builder = new StringBuilder();
                        for (String value : reasonArray) {
                            builder.append(value + " ");
                        }
                        reason = builder.substring(0, builder.toString().length() - 1);
                    } else {
                        reason = "None";
                    }

                    } catch (NullPointerException | IllegalArgumentException e) {
                        event.reply(Embeds.syntaxError("report @User/ID (Reason)", event));
                    }
                    
                    } else {

                        member = event.getMessage().getReferencedMessage().getMember();
                        reason = "None";

                    }

                    DateTimeFormatter dtw = DateTimeFormatter.ofPattern("EEE',' dd/MM/yyyy',' HH:mm", new Locale("en"));
                    LocalDateTime now = LocalDateTime.now();
                    MessageChannel reportChannel = Database.configChannel(event, "report_cid");

                    EmbedBuilder eb = new EmbedBuilder()
                            .setAuthor(member.getUser().getAsTag() + " | Report", null, member.getUser().getEffectiveAvatarUrl())
                            .setColor(new Color(0x2F3136))
                            .addField("Name", "```" + member.getUser().getAsTag() + "```", false)
                            .addField("ID", "```" + member.getId() + "```", true)
                            .addField("Reported by", "```" + event.getMessage().getAuthor().getAsTag() + "```", true)
                            .addField("Channel", "```#" + event.getMessage().getChannel().getName() + "```", true)
                            .addField("Reported on", "```" + dtw.format(now) + "```", true)
                            .addField("Reason", "```" + reason + "```", false)
                            .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                            .setTimestamp(new Date().toInstant());

                    reportChannel.sendMessage("@here").embed(eb.build()).queue();

                    eb.setDescription("Succesfully reported " + "``" + member.getUser().getAsTag() + "``!\nYour report has been send to our Moderators");
                    event.getAuthor().openPrivateChannel().complete().sendMessage(eb.build()).queue();

                    event.getMessage().delete().queue();
                    
        }
    }


