package com.javadiscord.javabot.commands.jam.phase_transitions;

import com.javadiscord.javabot.commands.jam.JamChannelManager;
import com.javadiscord.javabot.commands.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.commands.jam.dao.JamRepository;
import com.javadiscord.javabot.commands.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamPhase;
import com.javadiscord.javabot.commands.jam.model.JamSubmission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
import java.util.List;

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
