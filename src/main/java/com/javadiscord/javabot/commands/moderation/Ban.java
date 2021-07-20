package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import com.javadiscord.javabot.commands.moderation.actions.BanAction;

public class Ban implements SlashCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(Embeds.permissionError("BAN_MEMBERS", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }

        Member member = event.getOption("user").getAsMember();

        OptionMapping option = event.getOption("reason");
        String reason = option == null ? "None" : option.getAsString();

        new BanAction().handle(event, member, event.getUser(), reason);
    }
}