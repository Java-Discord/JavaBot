package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Kick implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Member member = event.getOption("user").getAsMember();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        String moderatorTag = event.getUser().getAsTag();

        var eb = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Kick", null, member.getUser().getEffectiveAvatarUrl())
            .setColor(Color.decode(Bot.config.get(event.getGuild()).getSlashCommand()
                        .getErrorColor()))
            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
            .addField("Moderator", "```" + moderatorTag + "```", true)
            .addField("ID", "```" + member.getId() + "```", false)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter("ID: " + member.getId())
            .setTimestamp(new Date().toInstant())
            .build();

        try {

            member.kick(reason).queueAfter(3, TimeUnit.SECONDS);
            new Database().setMemberEntry(member.getId(), "warns", 0);

            new Warn().deleteAllDocs(member.getId());

            Misc.sendToLog(event.getGuild(), eb);
            member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();
            return event.replyEmbeds(eb);
        } catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }
    }
}