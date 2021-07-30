package com.javadiscord.javabot.jam.phase_transitions;

import com.javadiscord.javabot.jam.JamChannelManager;
import com.javadiscord.javabot.jam.model.Jam;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;

public interface JamPhaseTransition {
	void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception;
}
