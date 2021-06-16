package com.javadiscord.javabot.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public interface SlashCommandHandler {
	void handle(SlashCommandEvent event);
}
