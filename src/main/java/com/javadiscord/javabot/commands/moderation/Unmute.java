package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.awt.*;
import java.time.Instant;

public class Unmute implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        Role muteRole = Bot.config.get(event.getGuild()).getModeration().getMuteRole();
        Member member = event.getOption("user").getAsMember();
        User author = event.getUser();
        try {
            var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Unmute", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

            if (member.getRoles().toString().contains(muteRole.getId())) {
                event.getGuild().removeRoleFromMember(member.getId(), muteRole).complete();

                member.getUser().openPrivateChannel().complete().sendMessageEmbeds(e).queue();

                Misc.sendToLog(event.getGuild(), e);
                return event.replyEmbeds(e);
            } else return Responses.error(event, "```Can't unmute " + member.getUser().getAsTag() + ", they aren't muted.```");

        } catch (HierarchyException e) {
            return Responses.error(event, e.getMessage());
        }
    }
}
