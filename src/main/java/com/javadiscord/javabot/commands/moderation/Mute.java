package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.moderation.actions.MuteAction;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
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

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        new MuteAction().handle(event, member, event.getUser(), reason);
    }
}