package net.javadiscord.javabot.systems.jam.phase_transitions;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.dao.JamMessageRepository;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.model.JamTheme;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Moves the jam from theme planning to theme voting.
 * <ol>
 *     <li>Get the list of themes.</li>
 *     <li>Send a message in the voting channel so that users can vote on the themes.</li>
 *     <li>Save the message's id to the database under the "theme_voting" name.</li>
 *     <li>Mark the jam as in the voting phase.</li>
 * </ol>
 */
public class ToThemeVotingTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws SQLException {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(jam);
		if (themes.isEmpty()) {
			throw new IllegalStateException("Cannot start theme voting until at least one theme is available.");
		}
		long votingMessageId = channelManager.sendThemeVotingMessages(jam, themes);
		new JamMessageRepository(con).saveMessageId(jam, votingMessageId, "theme_voting");
		new JamRepository(con).updateJamPhase(jam, JamPhase.THEME_VOTING);
	}
}
