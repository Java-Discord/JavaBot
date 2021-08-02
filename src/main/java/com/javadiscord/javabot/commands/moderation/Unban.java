package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Date;

public class Unban implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            return event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
        String id = event.getOption("id").getAsString();
        User author = event.getUser();

        try {
            event.getGuild().unban(id).complete();
            var e = new EmbedBuilder()
                .setAuthor("Unban")
                .setColor(Constants.RED)
                .addField("ID", "```" +id + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .setFooter("ID: " + id)
                .setTimestamp(new Date().toInstant())
                .build();


            Misc.sendToLog(event.getGuild(), e);
            return event.replyEmbeds(e);
        } catch (ErrorResponseException e) {
            return event.replyEmbeds(Embeds.emptyError("```User (" + id + ") not found.```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }
}

