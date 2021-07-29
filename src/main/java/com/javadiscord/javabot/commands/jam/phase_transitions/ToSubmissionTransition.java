package com.javadiscord.javabot.commands.jam.phase_transitions;

import com.javadiscord.javabot.commands.jam.JamChannelManager;
import com.javadiscord.javabot.commands.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToSubmissionTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception {
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