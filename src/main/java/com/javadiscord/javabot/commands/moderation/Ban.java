package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Ban implements SlashCommandHandler {

    public void ban (Member member, String reason) throws Exception {

        new Warn().deleteAllDocs(member.getId());
        member.ban(6, reason).queue();
    }

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        Member member = event.getOption("user").getAsMember();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        var eb = new EmbedBuilder()
                .setColor(Constants.RED)
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        event.replyEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

        try { ban(member, reason); }
        catch (Exception e) { event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).queue(); }
    }
}