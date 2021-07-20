package com.javadiscord.javabot.commands.moderation.actions;

import com.javadiscord.javabot.commands.moderation.Warn;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BanAction implements ActionHandler {

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
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + author.getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        try {
            member.ban(6, reason).queueAfter(3, TimeUnit.SECONDS);
            WarnAction.deleteAllDocs(member.getId());

            if (slash) {
                net.dv8tion.jda.api.events.interaction.SlashCommandEvent event = (SlashCommandEvent) ev;
                event.replyEmbeds(eb).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            } else {
                net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) ev;
                event.getChannel().sendMessageEmbeds(eb).queue();
            }

            Misc.sendToLog(guild, eb);
            member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

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
