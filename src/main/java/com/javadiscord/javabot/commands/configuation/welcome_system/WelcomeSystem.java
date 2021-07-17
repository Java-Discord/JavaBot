package com.javadiscord.javabot.commands.configuation.welcome_system;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.welcome_system.subcommands.*;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.Map;


public class WelcomeSystem implements SlashCommandHandler, WelcomeCommandHandler {

    private final Map<String, WelcomeCommandHandler> welcomeIndex;

    public WelcomeSystem() {

        this.welcomeIndex = new HashMap<>();

        welcomeIndex.put("list", new GetList());
        welcomeIndex.put("leave-msg", new SetLeaveMessage());
        welcomeIndex.put("join-msg", new SetJoinMessage());
        welcomeIndex.put("channel",new SetWelcomeChannel());
        welcomeIndex.put("image-width", new SetImageWidth());
        welcomeIndex.put("image-height", new SetImageHeight());
        welcomeIndex.put("overlay-url", new SetOverlayUrl());
        welcomeIndex.put("background-url", new SetBackgroundUrl());
        welcomeIndex.put("primary-color", new SetPrimaryColor());
        welcomeIndex.put("secondary-color", new SetSecondaryColor());
        welcomeIndex.put("avatar-height", new SetAvatarHeight());
        welcomeIndex.put("avatar-width", new SetAvatarWidth());
        welcomeIndex.put("avatar-x", new SetAvatarX());
        welcomeIndex.put("avatar-y", new SetAvatarY());
        welcomeIndex.put("status", new SetStatus());
    }

    @Override
    public void handle(SlashCommandEvent event) {

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            return;
        }
            var command = welcomeIndex.get(event.getSubcommandName());
            if (command != null) {
                command.handle(event);
            return;
        }
    }
}

