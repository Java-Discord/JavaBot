package com.javadiscord.javabot.other;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public abstract class SlashEnabledCommand extends Command {
	@Override
	protected void execute(CommandEvent event) {
		this.execute(new SlashEnabledCommandEvent(event));
	}

	public void execute(SlashCommandEvent event) {
		this.execute(new SlashEnabledCommandEvent(event));
	}

	protected abstract void execute(SlashEnabledCommandEvent event);
}
