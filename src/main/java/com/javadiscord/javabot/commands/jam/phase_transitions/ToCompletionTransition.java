package com.javadiscord.javabot.commands.jam.phase_transitions;

import com.javadiscord.javabot.commands.jam.JamChannelManager;
import com.javadiscord.javabot.commands.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamSubmission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ToCompletionTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception {
		JamMessageRepository messageRepository = new JamMessageRepository(con);
		List<JamSubmission> submissions = new JamSubmissionRepository(con).getSubmissions(jam);

		var votes = channelManager.getSubmissionVotes(this.getSubmissionMessageIds(submissions, messageRepository));
		var voteCounts = this.recordAndCountVotes(votes, con, messageRepository);
		var winningSubmissions = this.determineWinners(voteCounts);

		if (winningSubmissions.isEmpty()) {
			channelManager.sendNoWinnersMessage();
		} else if (winningSubmissions.size() == 1) {
			channelManager.sendSingleWinnerMessage(winningSubmissions.get(0), voteCounts, event);
		} else {
			channelManager.sendMultipleWinnersMessage(winningSubmissions, voteCounts, event);
		}
		new JamRepository(con).completeJam(jam);
	}

	public List<JamSubmission> determineWinners(Map<JamSubmission, Integer> voteCounts) {
		int highestVoteCount = voteCounts.values().stream().max(Comparator.naturalOrder()).orElse(-1);
		return voteCounts.entrySet().stream()
				.filter(entry -> entry.getValue() == highestVoteCount)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	public Map<JamSubmission, Integer> recordAndCountVotes(Map<JamSubmission, List<Long>> submissionVotes, Connection con, JamMessageRepository messageRepository) throws SQLException {
		PreparedStatement submissionVoteStmt = con.prepareStatement("INSERT INTO jam_submission_vote (submission_id, user_id) VALUES (?, ?)");
		Map<JamSubmission, Integer> voteCounts = new HashMap<>();
		for (var entry : submissionVotes.entrySet()) {
			submissionVoteStmt.setLong(1, entry.getKey().getId());
			for (var userId : entry.getValue()) {
				submissionVoteStmt.setLong(2, userId);
				submissionVoteStmt.executeUpdate();
			}
			voteCounts.put(entry.getKey(), entry.getValue().size());
			messageRepository.removeMessageId(entry.getKey().getJam(), "submission-" + entry.getKey().getId());
		}
		return voteCounts;
	}

	public Map<JamSubmission, Long> getSubmissionMessageIds(List<JamSubmission> submissions, JamMessageRepository messageRepository) throws SQLException {
		Map<JamSubmission, Long> submissionMessages = new HashMap<>();
		for (JamSubmission submission : submissions) {
			Long id = messageRepository.getMessageId(submission.getJam(), "submission-" + submission.getId());
			if (id == null) {
				throw new IllegalStateException("Could not find message id for submission " + submission.getId());
			}
			submissionMessages.put(submission, id);
		}
		return submissionMessages;
	}
}
