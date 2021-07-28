package com.javadiscord.javabot.commands.jam.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.jam.JamCommandHandler;
import com.javadiscord.javabot.commands.jam.JamDataManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

@RequiredArgsConstructor
public abstract class ActiveJamSubcommand implements SlashCommandHandler {
	protected final JamDataManager dataManager;
	private final boolean admin;

	@Override
	public void handle(SlashCommandEvent event) {
		if (admin && !JamCommandHandler.ensureAdmin(event)) return;
		event.deferReply().queue();

		Jam activeJam = this.dataManager.getActiveJam(event.getGuild().getIdLong());
		if (activeJam == null) {
			event.getHook().sendMessage("No active Jam in this guild.").queue();
			return;
		}

		try {
			this.handleJamCommand(event, activeJam);
		} catch (Exception e) {
			event.getHook().sendMessage("An error occurred: " + e.getMessage()).queue();
			e.printStackTrace();
		}
	}

	protected abstract void handleJamCommand(SlashCommandEvent event, Jam activeJam) throws Exception;
}
