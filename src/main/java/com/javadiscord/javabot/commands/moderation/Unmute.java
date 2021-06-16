package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Date;

public class Unmute {

    public static void execute(SlashCommandEvent event, Member member, User author) {
            if (event.getMember().hasPermission(Permission.MANAGE_ROLES)) {

                    Role muteRole = Database.configRole(event, "mute_rid");

                    try {
                        event.getGuild().removeRoleFromMember(member.getId(), muteRole).complete();

                        var e = new EmbedBuilder()
                                .setAuthor(member.getUser().getAsTag() + " | Unmute", null, member.getUser().getEffectiveAvatarUrl())
                                .setColor(Constants.RED)
                                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                                .addField("ID", "```" + member.getId() + "```", false)
                                .setFooter("ID: " + member.getId())
                                .setTimestamp(new Date().toInstant())
                                .build();

                        member.getUser().openPrivateChannel().complete().sendMessage(e).queue();
                        Misc.sendToLog(event, e);
                        event.replyEmbeds(e).queue();

                    } catch (HierarchyException e) { event.replyEmbeds(Embeds.hierarchyError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
                    } else { event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
        }
    }
