package com.javadiscord.javabot.jam;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamPhase;
import com.javadiscord.javabot.jam.phase_transitions.*;
import com.javadiscord.javabot.other.Database;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The phase manager is responsible for the logic that is required to transition
 * to each phase of the Jam.
 */
public class JamPhaseManager {
	private static final Logger log = LoggerFactory.getLogger(JamPhaseManager.class);
	public static final String[] REACTION_NUMBERS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	public static final String SUBMISSION_VOTE_UNICODE = "⬆";

	/**
	 * Moves the jam to the next phase, or completes the jam if it's at the end
	 * of its life cycle.
	 * @param jam The jam to update.
	 * @param event The event which triggered this action.
	 */
	public void nextPhase(Jam jam, SlashCommandEvent event) {
		JamPhaseTransition transition = null;
		switch (jam.getCurrentPhase()) {
			case JamPhase.THEME_PLANNING:
				transition = new ToThemeVotingTransition();
				break;
			case JamPhase.THEME_VOTING:
				transition = new ToSubmissionTransition();
				break;
			case JamPhase.SUBMISSION:
				transition = new ToSubmissionVotingTransition();
				break;
			case JamPhase.SUBMISSION_VOTING:
				transition = new ToCompletionTransition();
				break;
		}
		if (transition != null) {
			this.doTransition(transition, jam, event);
		}
	}

	private void doTransition(JamPhaseTransition transition, Jam jam, SlashCommandEvent event) {
		new Thread(() -> {
			var channelManager = new JamChannelManager(event.getGuild(), new Database());
			Connection c;
			try {
				c = Bot.dataSource.getConnection();
				c.setAutoCommit(false);
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			}

			try {
				transition.transition(jam, event, channelManager, c);
				c.commit();
			} catch (Exception e) {
				try {
					c.rollback();
				} catch (SQLException ex) {
					log.error("SEVERE ERROR: Could not rollback changes made during a failed transition to new Jam state.");
					ex.printStackTrace();
				}
				log.error("An error occurred while transitioning the Jam phase: ", e);
				channelManager.sendErrorMessageAsync(event, "An error occurred: " + e.getMessage());
			}

			try {
				c.close();
			} catch (SQLException e) {
				log.error("Could not close connecting while transitioning the Jam phase: ", e);
			}
		}).start();
	}
}
