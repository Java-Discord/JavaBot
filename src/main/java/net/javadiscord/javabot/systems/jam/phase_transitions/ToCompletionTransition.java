package net.javadiscord.javabot.systems.jam.phase_transitions;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.dao.JamMessageRepository;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transitions the jam to completion, that is, it does the following things.
 * <ol>
 *     <li>Counts the number of votes each submission received.</li>
 *     <li>Determine the winner(s) of the jam.</li>
 *     <li>Send an announcement about the winner(s).</li>
 *     <li>Remove all stored "submission-[id]" message ids from the database.</li>
 *     <li>Mark the jam as completed.</li>
 * </ol>
 */
public class ToCompletionTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws SQLException {
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

	/**
	 * Determines the JavaJam Winner by their votecount.
	 *
	 * @param voteCounts A Map containing the {@link JamSubmission} and an Integer that represents the vote counts.
	 * @return The JavaJam winners.
	 */
	public List<JamSubmission> determineWinners(Map<JamSubmission, Integer> voteCounts) {
		int highestVoteCount = voteCounts.values().stream().max(Comparator.naturalOrder()).orElse(-1);
		return voteCounts.entrySet().stream()
				.filter(entry -> entry.getValue() == highestVoteCount)
				.map(Map.Entry::getKey)
				.toList();
	}

	/**
	 * Counts JavaJam submission votes.
	 *
	 * @param submissionVotes   A Map containing the {@link JamSubmission} and a {@link List} that contains all message id's.
	 * @param con               The datasource's connection.
	 * @param messageRepository The {@link JamMessageRepository}.
	 * @return A Map containing the {@link JamSubmission} and the vote count as an Integer.
	 * @throws SQLException If an error occurs.
	 */
	public Map<JamSubmission, Integer> recordAndCountVotes(Map<JamSubmission, List<Long>> submissionVotes, Connection con, JamMessageRepository messageRepository) throws SQLException {
		try (PreparedStatement submissionVoteStmt = con.prepareStatement("INSERT INTO jam_submission_vote (submission_id, user_id) VALUES (?, ?)")) {
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
	}

	/**
	 * Retrieves the submission's message ids.
	 *
	 * @param submissions       A {@link List} with all Submissions.
	 * @param messageRepository The {@link JamMessageRepository}.
	 * @return A Map contains the {@link JamSubmission} and its message id.
	 * @throws SQLException If an error occurs.
	 */
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
