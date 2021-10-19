package com.javadiscord.javabot.service.jam.phase_transitions;

import com.javadiscord.javabot.service.jam.JamChannelManager;
import com.javadiscord.javabot.service.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.service.jam.dao.JamRepository;
import com.javadiscord.javabot.service.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.service.jam.model.Jam;
import com.javadiscord.javabot.service.jam.model.JamPhase;
import com.javadiscord.javabot.service.jam.model.JamSubmission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
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
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception {
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
