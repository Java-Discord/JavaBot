package com.javadiscord.javabot.commands.moderation.actions;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.util.Date;

public class MuteAction implements ActionHandler {

    @Override
    public void handle(Object ev, Member member, User author, String reason) {

        Guild guild = null;
        boolean slash = false;

        if (ev instanceof net.dv8tion.jda.api.events.interaction.SlashCommandEvent) {
            net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;

            guild = event.getGuild();
            slash = true;
        }

        if (ev instanceof net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent) {
            net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;

            guild = event.getGuild();
            slash = false;
        }

        var eb = new EmbedBuilder()
                .setColor(Constants.RED)
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        try {
            Role muteRole = guild.getRoleById(Database.getConfigString(guild.getName(), guild.getId(), "roles.mute_rid"));

            if (!(member.getRoles().toString().contains(muteRole.getId()))) {
                guild.addRoleToMember(member.getId(), muteRole).complete();

                member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();
                Misc.sendToLog(guild, eb);

                if (slash) {
                    net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                    event.replyEmbeds(eb).queue();
                } else {
                    net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                    event.getChannel().sendMessageEmbeds(eb).queue();
                }

            } else {

                if (slash) {
                    net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                    event.replyEmbeds(Embeds.emptyError("```" + member.getUser().getAsTag() + " is already muted```", author)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                } else {
                    net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                    event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + member.getUser().getAsTag() + " is already muted```", author)).queue();
                }
            }


        } catch (Exception e) {

            if (slash) {
                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", author)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            } else {
                net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                event.getChannel().sendMessageEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", author)).queue();
            }
        }
    }
}
