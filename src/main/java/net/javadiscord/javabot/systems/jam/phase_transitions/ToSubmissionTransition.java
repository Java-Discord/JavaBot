package net.javadiscord.javabot.systems.jam.phase_transitions;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.dao.JamMessageRepository;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamThemeRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.model.JamTheme;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Moves the jam from the theme voting phase to the submission phase, by doing
 * the following.
 * <ol>
 *     <li>Counts the number of votes for each theme.</li>
 *     <li>Determine the winning theme(s).</li>
 *     <li>Mark all themes as either accepted or not accepted.</li>
 *     <li>Send an announcement about the winning theme(s).</li>
 *     <li>Mark the jam as in the submission phase.</li>
 *     <li>Remove the "theme_voting" message id from the database.</li>
 * </ol>
 */
public class ToSubmissionTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandInteractionEvent event, JamChannelManager channelManager, Connection con) throws SQLException {
		JamMessageRepository messageRepository = new JamMessageRepository(con);
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(jam);
		long themeVotingMessageId = messageRepository.getMessageId(jam, "theme_voting");
		var votes = channelManager.getThemeVotes(themeVotingMessageId, themes);
		var voteCounts = this.recordAndCountVotes(jam, votes, con);
		JamTheme winningTheme = this.determineWinner(voteCounts);
		if (winningTheme == null) {
			throw new IllegalStateException("No winning jam theme could be found.");
		}

		this.updateThemesAcceptedState(themes, winningTheme, con);
		channelManager.sendChosenThemeMessage(voteCounts, winningTheme);
		new JamRepository(con).updateJamPhase(jam, JamPhase.SUBMISSION);
		messageRepository.removeMessageId(jam, "theme_voting");
	}

	private JamTheme determineWinner(Map<JamTheme, Integer> voteCounts) {
		JamTheme winningTheme = null;
		int winningThemeVotes = 0;
		for (var entry : voteCounts.entrySet()) {
			if (entry.getValue() > winningThemeVotes) {
				winningTheme = entry.getKey();
				winningThemeVotes = entry.getValue();
			}
		}
		return winningTheme;
	}

	private Map<JamTheme, Integer> recordAndCountVotes(Jam jam, Map<JamTheme, List<Long>> votes, Connection con) throws SQLException {
		Map<JamTheme, Integer> voteCounts = new HashMap<>();
		PreparedStatement themeVoteStmt = con.prepareStatement("INSERT INTO jam_theme_vote (jam_id, theme_name, user_id) VALUES (?, ?, ?)");
		themeVoteStmt.setLong(1, jam.getId());
		for (var entry : votes.entrySet()) {
			JamTheme theme = entry.getKey();
			themeVoteStmt.setString(2, theme.getName());
			for (long userId : entry.getValue()) {
				themeVoteStmt.setLong(3, userId);
				themeVoteStmt.executeUpdate();
			}
			voteCounts.put(theme, entry.getValue().size());
		}
		return voteCounts;
	}

	private void updateThemesAcceptedState(List<JamTheme> themes, JamTheme winner, Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement("UPDATE jam_theme SET accepted = ? WHERE jam_id = ? AND name = ?");
		for (JamTheme theme : themes) {
			stmt.setBoolean(1, theme.equals(winner));
			stmt.setLong(2, theme.getJam().getId());
			stmt.setString(3, theme.getName());
			stmt.executeUpdate();
			theme.setAccepted(theme.equals(winner));
		}
		stmt.close();
	}
}
