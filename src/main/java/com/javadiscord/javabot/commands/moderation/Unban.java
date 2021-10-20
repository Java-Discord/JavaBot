package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.utils.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;

public class Unban implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        String id = event.getOption("id").getAsString();
        User author = event.getUser();

        event.getGuild().unban(id).queue(unused->{
            var e = new EmbedBuilder()
                    .setAuthor("Unban")
                    .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor())
                    .addField("ID", "```" + id + "```", true)
                    .addField("Moderator", "```" + author.getAsTag() + "```", true)
                    .setFooter("ID: " + id)
                    .setTimestamp(Instant.now())
                    .build();

                Misc.sendToLog(event.getGuild(), e);
                event.replyEmbeds(e).queue();
        }, e -> Responses.error(event, "```User (" + id + ") not found.```").queue());
        
        return event.deferReply();
    }
}

