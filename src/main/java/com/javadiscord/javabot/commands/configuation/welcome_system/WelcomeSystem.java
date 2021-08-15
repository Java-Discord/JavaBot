package com.javadiscord.javabot.commands.configuation.welcome_system;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;
import com.javadiscord.javabot.commands.configuation.welcome_system.subcommands.*;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;


public class WelcomeSystem extends DelegatingCommandHandler {
    public WelcomeSystem() {
        addSubcommand("list", new GetList());
        addSubcommand("leave-msg", new SetLeaveMessage());
        addSubcommand("join-msg", new SetJoinMessage());
        addSubcommand("channel",new SetWelcomeChannel());
        addSubcommand("image-width", new SetImageWidth());
        addSubcommand("image-height", new SetImageHeight());
        addSubcommand("overlay-url", new SetOverlayUrl());
        addSubcommand("background-url", new SetBackgroundUrl());
        addSubcommand("primary-color", new SetPrimaryColor());
        addSubcommand("secondary-color", new SetSecondaryColor());
        addSubcommand("avatar-height", new SetAvatarHeight());
        addSubcommand("avatar-width", new SetAvatarWidth());
        addSubcommand("avatar-x", new SetAvatarX());
        addSubcommand("avatar-y", new SetAvatarY());
        addSubcommand("status", new SetStatus());
    }

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
        return super.handle(event);
    }
}

