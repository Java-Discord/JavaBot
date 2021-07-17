package com.javadiscord.javabot.commands.configuation.config;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public interface ConfigCommandHandler {

    void handle(SlashCommandEvent event);
}
