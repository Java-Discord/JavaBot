package net.javadiscord.javabot.systems.jam.phase_transitions;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.systems.jam.JamChannelManager;
import net.javadiscord.javabot.systems.jam.dao.JamMessageRepository;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.dao.JamSubmissionRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.model.JamSubmission;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Moves the jam from submission to submission voting. This involves:
 * <ol>
 *     <li>Get a list of all of the most recent submissions from each user.</li>
 *     <li>Create a message for each submission so that users can vote on it.</li>
 *     <li>Save a "submission-[id]" message to the database for each submission's voting message.</li>
 *     <li>Mark the jam as in the submission voting phase.</li>
 * </ol>
 */
public class ToSubmissionVotingTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws SQLException {
		List<JamSubmission> submissions = new JamSubmissionRepository(con).getSubmissions(jam);
		if (submissions.isEmpty()) {
			throw new IllegalStateException("Cannot start submission voting because there aren't any submissions.");
		}
		JamMessageRepository messageRepository = new JamMessageRepository(con);
		var messageIds = channelManager.sendSubmissionVotingMessage(jam, submissions, event.getJDA());
		for (var entry : messageIds.entrySet()) {
			messageRepository.saveMessageId(jam, entry.getValue(), "submission-" + entry.getKey().getId());
		}
		new JamRepository(con).updateJamPhase(jam, JamPhase.SUBMISSION_VOTING);
	}
}
