package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.Date;
import java.util.Objects;

public class Unmute implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            return event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
        Role muteRole = Bot.config.getModeration().getMuteRole(Objects.requireNonNull(event.getGuild()));
        Member member = event.getOption("user").getAsMember();
        User author = event.getUser();
        try {
            var e = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Unmute", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.RED)
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

            if (member.getRoles().toString().contains(muteRole.getId())) {
                event.getGuild().removeRoleFromMember(member.getId(), muteRole).complete();

                member.getUser().openPrivateChannel().complete().sendMessage(e).queue();

                Misc.sendToLog(event.getGuild(), e);
                return event.replyEmbeds(e);
            } else {
                return event.replyEmbeds(Embeds.emptyError("```I can't unmute " + member.getUser().getAsTag() + ", they aren't muted.```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL);
            }

        } catch (HierarchyException e) {
            return event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", author)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }
}
