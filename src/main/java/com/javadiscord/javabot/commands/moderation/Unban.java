package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.Date;

public class Unban implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        String id = event.getOption("id").getAsString();
        User author = event.getUser();

        try {
            event.getGuild().unban(id).complete();
            var e = new EmbedBuilder()
                .setAuthor("Unban")
                .setColor(Color.decode(Bot.config.get(event.getGuild()).getSlashCommand()
                            .getErrorColor()))
                .addField("ID", "```" + id + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .setFooter("ID: " + id)
                .setTimestamp(new Date().toInstant())
                .build();


            Misc.sendToLog(event.getGuild(), e);
            return event.replyEmbeds(e);
        } catch (ErrorResponseException e) {
            return Responses.error(event, "```User (" + id + ") not found.```");
        }
    }
}

