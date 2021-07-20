package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.Date;

public class ClearWarns implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            event.replyEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        Member member = event.getOption("user").getAsMember();

        Database.queryMember(member.getId(), "warns", 0);
        new Warn().deleteAllDocs(member.getId());

        var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Warns cleared", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.YELLOW)
                .setDescription("Succesfully cleared all warns from " + member.getUser().getAsMention() + ".")
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        event.replyEmbeds(e).queue();

    }
}

