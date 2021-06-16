package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.Date;

public class Unban {

    public static void execute(SlashCommandEvent event, String id, User author) {
        if (event.getMember().hasPermission(Permission.BAN_MEMBERS)) {

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

                event.replyEmbeds(e).queue();
                Misc.sendToLog(event, e);

            } catch (ErrorResponseException e) {
                event.replyEmbeds(Embeds.emptyError("```User (" + id + ") not found.```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            }

        } else { event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
    }
}

