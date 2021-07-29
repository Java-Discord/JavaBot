package com.javadiscord.javabot.commands.jam.phase_transitions;

import com.javadiscord.javabot.commands.jam.JamChannelManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;

public interface JamPhaseTransition {
	void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception;
}
