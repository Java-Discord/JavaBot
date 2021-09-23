package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.time.Instant;

public class Ban implements SlashCommandHandler {

    public void ban (Member member, String reason) {
        new Warn().deleteAllDocs(member.getId());
        member.ban(6, reason).queue();
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

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
                .setTimestamp(Instant.now())
                .build();

        try {
            ban(member, reason);

            Misc.sendToLog(event.getGuild(), eb);
            if (member.getUser().hasPrivateChannel()) member.getUser().openPrivateChannel().complete().
                    sendMessageEmbeds(eb).queue();
        }
        catch (Exception e) {
            return Responses.error(event, e.getMessage());
        }

        return event.replyEmbeds(eb);
    }
}