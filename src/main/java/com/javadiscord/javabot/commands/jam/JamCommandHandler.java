package com.javadiscord.javabot.commands.jam;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class JamCommandHandler implements SlashCommandHandler {
	@Override
	public void handle(SlashCommandEvent event) {
		event.reply("Not yet implemented.").queue();
	}
}
