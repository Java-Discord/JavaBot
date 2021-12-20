package net.javadiscord.javabot.systems.jam.phase_transitions;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.model.Jam;

import java.sql.Connection;

/**
 * Represents an atomic transition from one Jam state to another. For example,
 * going from the theme voting phase to the submission phase. During the
 * transition method, an exception may be thrown at any time to roll back the
 * transition.
 */
public interface JamPhaseTransition {
	void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception;
}
