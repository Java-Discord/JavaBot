package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Ban implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        Member member = event.getOption("user").getAsMember();
        String modTag = event.getUser().getAsTag();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        var eb = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
            .setColor(Constants.RED)
            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
            .addField("Moderator", "```" + modTag + "```", true)
            .addField("ID", "```" + member.getId() + "```", false)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter("ID: " + member.getId())
            .setTimestamp(new Date().toInstant())
            .build();

        TextChannel tc;

        try {
            member.ban(6, reason).queueAfter(3, TimeUnit.SECONDS);
            Warn.deleteAllDocs(member.getId());
            tc = event.getTextChannel();
            if (reason.equalsIgnoreCase("3/3 warns")) tc.sendMessage(eb).queue();
            else event.replyEmbeds(eb).queue();
            Misc.sendToLog(event, eb);
            member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();


        } catch (HierarchyException e) {
            event.replyEmbeds(Embeds.hierarchyError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        } catch (NullPointerException | NumberFormatException e) {
            e.printStackTrace();
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}