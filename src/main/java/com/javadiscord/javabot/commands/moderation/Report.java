package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.time.LocalDateTime;
import java.util.Date;

public class Report {

    public static void execute(SlashCommandEvent event, Member member, User author, String reason) {

        MessageChannel reportChannel = Database.configChannel(event, "report_cid");

        var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Report", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.GRAY)
                .addField("Name", "```" + member.getUser().getAsTag() + "```", false)
                .addField("ID", "```" + member.getId() + "```", true)
                .addField("Reported by", "```" + author.getAsTag() + "```", true)
                .addField("Channel", "```#" + event.getTextChannel().getName() + "```", true)
                .addField("Reported on", "```" + LocalDateTime.now().format(TimeUtils.STANDARD_FORMATTER) + "```", true)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter(author.getAsTag(), author.getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant());

        reportChannel.sendMessage("@here").embed(e.build()).queue();

        e.setDescription("Succesfully reported " + "``" + member.getUser().getAsTag() + "``!\nYour report has been send to our Moderators");
        event.replyEmbeds(e.build()).setEphemeral(true).queue();
        }
    }


