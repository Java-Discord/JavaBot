package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Date;


public class Mute {

    public static void mute(Member member, String moderatorTag, Object ev) {

        var eb = new EmbedBuilder()
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .setColor(Constants.RED)
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + moderatorTag + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        Guild guild = null;
        TextChannel tc = null;

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
            tc = event.getChannel();
        }

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            guild = event.getGuild();
            tc = event.getTextChannel();
        }

        try {

            Role muteRole = Database.configRole(ev, "mute_rid");

            if (!(member.getRoles().toString().contains(muteRole.getId()))) {
                guild.addRoleToMember(member.getId(), muteRole).complete();

                Misc.sendToLog(ev, eb);
                member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();

                if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
                    net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                    event.replyEmbeds(eb).queue();

                } else { tc.sendMessage(eb).queue(); }

            }

        } catch (HierarchyException e) {

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {

                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.hierarchyError(ev)).setEphemeral(Constants.ERR_EPHEMERAL).queue();

            } else { tc.sendMessage(Embeds.hierarchyError(ev)).queue(); }

        } catch (NullPointerException | NumberFormatException e) {

            if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {

                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", ev)).setEphemeral(Constants.ERR_EPHEMERAL).queue();

            } else { tc.sendMessage(Embeds.emptyError("```" + e.getMessage() + "```", ev)); }
        }
    }


        public static void execute(SlashCommandEvent event, Member member, User author) {
            if (event.getMember().hasPermission(Permission.MANAGE_ROLES)) {

                mute(member, author.getAsTag(), event);

            } else { event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue(); }
        }
    }