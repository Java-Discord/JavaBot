package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Kick {

    public static void kick(Member member, String reason, String moderatorTag, SlashCommandEvent event){

        var eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Kick", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.RED)
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + moderatorTag + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        try {

            member.kick(reason).queueAfter(3, TimeUnit.SECONDS);

            Misc.sendToLog(event, eb);
            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
            event.replyEmbeds(eb).queue();

        } catch (HierarchyException e) {
            event.replyEmbeds(Embeds.hierarchyError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }

    public static void execute(SlashCommandEvent event, Member member, User author, String reason) {
        if (event.getMember().hasPermission(Permission.KICK_MEMBERS)) {

                kick(member, reason, author.getAsTag(), event);

            } else { event.replyEmbeds(Embeds.permissionError("KICK_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }
}