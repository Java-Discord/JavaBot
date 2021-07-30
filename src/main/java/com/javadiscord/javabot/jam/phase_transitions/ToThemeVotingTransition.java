package com.javadiscord.javabot.jam.phase_transitions;

import com.javadiscord.javabot.jam.JamChannelManager;
import com.javadiscord.javabot.jam.dao.JamMessageRepository;
import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamPhase;
import com.javadiscord.javabot.jam.model.JamTheme;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.sql.Connection;
import java.util.List;

public class ToThemeVotingTransition implements JamPhaseTransition {
	@Override
	public void transition(Jam jam, SlashCommandEvent event, JamChannelManager channelManager, Connection con) throws Exception {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(jam);
		if (themes.isEmpty()) throw new IllegalStateException("Cannot start theme voting until at least one theme is available.");
		long votingMessageId = channelManager.sendThemeVotingMessages(jam, themes);
		new JamMessageRepository(con).saveMessageId(jam, votingMessageId, "theme_voting");
		new JamRepository(con).updateJamPhase(jam, JamPhase.THEME_VOTING);
	}
}
