package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Date;

public class Mute implements SlashCommandHandler {

    public void mute (Member member, Guild guild) throws Exception {

        Role muteRole = guild.getRoleById(new Database().getConfigString(guild, "roles.mute_rid"));
        guild.addRoleToMember(member.getId(), muteRole).complete();
    }

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        Member member = event.getOption("user").getAsMember();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        var eb = new EmbedBuilder()
                .setColor(Constants.RED)
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(new Date().toInstant())
                .build();

        event.replyEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().complete().sendMessageEmbeds(eb).queue();

        Role muteRole = event.getGuild().getRoleById(new Database().getConfigString(event.getGuild(), "roles.mute_rid"));

        if (member.getRoles().contains(muteRole)) {
            event.replyEmbeds(Embeds.emptyError("```" + member.getUser().getAsTag() + " is already muted```", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        try { mute(member, event.getGuild()); }
        catch (Exception e) { event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event.getUser())).queue(); }
    }
}