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
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Date;

public class Mute implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.replyEmbeds(Embeds.permissionError("MANAGE_ROLES", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }
        Member member = event.getOption("user").getAsMember();
        String moderatorTag = event.getUser().getAsTag();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        var eb = new EmbedBuilder()
            .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
            .setColor(Constants.RED)
            .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
            .addField("Moderator", "```" + moderatorTag + "```", true)
            .addField("ID", "```" + member.getId() + "```", false)
            .addField("Reason", "```" + reason + "```", false)
            .setFooter("ID: " + member.getId())
            .setTimestamp(new Date().toInstant())
            .build();

        Guild guild = event.getGuild();
        try {
            Role muteRole = Database.configRole(event, "mute_rid");
            if (!(member.getRoles().toString().contains(muteRole.getId()))) {
                guild.addRoleToMember(member.getId(), muteRole).complete();
                Misc.sendToLog(event, eb);
                member.getUser().openPrivateChannel().complete().sendMessage(eb).queue();
                event.replyEmbeds(eb).queue();
            }

        } catch (HierarchyException e) {
            event.replyEmbeds(Embeds.hierarchyError(event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        } catch (NullPointerException | NumberFormatException e) {
            event.replyEmbeds(Embeds.emptyError("```" + e.getMessage() + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}