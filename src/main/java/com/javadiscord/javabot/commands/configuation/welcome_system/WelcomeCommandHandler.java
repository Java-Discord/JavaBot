package com.javadiscord.javabot.commands.configuation.welcome_system;

import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.util.HashMap;
import java.util.Map;

public interface WelcomeCommandHandler {

    void handle(SlashCommandEvent event);
}