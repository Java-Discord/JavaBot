package net.javadiscord.javabot.systems.jam;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.model.JamPhase;
import net.javadiscord.javabot.systems.jam.phase_transitions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The phase manager is responsible for the logic that is required to transition
 * to each phase of the Jam.
 */
@RequiredArgsConstructor
public class JamPhaseManager {
	public static final String[] REACTION_NUMBERS = {"1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
	public static final String SUBMISSION_VOTE_UNICODE = "⬆";
	private static final Logger log = LoggerFactory.getLogger(JamPhaseManager.class);
	private final Jam jam;
	private final SlashCommandEvent event;
	private final JamChannelManager channelManager;

	/**
	 * Moves the jam to the next phase, or completes the jam if it's at the end
	 * of its life cycle.
	 */
	public void nextPhase() {
		JamPhaseTransition transition = switch (jam.getCurrentPhase()) {
			case JamPhase.THEME_PLANNING -> new ToThemeVotingTransition();
			case JamPhase.THEME_VOTING -> new ToSubmissionTransition();
			case JamPhase.SUBMISSION -> new ToSubmissionVotingTransition();
			case JamPhase.SUBMISSION_VOTING -> new ToCompletionTransition();
			default -> null;
		};
		if (transition != null) {
			this.doTransition(transition);
		}
	}

	private void doTransition(JamPhaseTransition transition) {
		new Thread(() -> {
			Connection bkpCon = null;
			try (Connection c = Bot.dataSource.getConnection()) {
				c.setAutoCommit(false);
				bkpCon = c;
				transition.transition(jam, event, channelManager, c);
				c.commit();
			} catch (Exception e) {
				log.error("An error occurred while transitioning the Jam phase.", e);
				channelManager.sendErrorMessageAsync(event, "An error occurred: " + e.getMessage());
				try {
					if (bkpCon != null) bkpCon.rollback();
				} catch (SQLException ex) {
					log.error("SEVERE ERROR: Could not rollback changes made during a failed transition to new Jam state.", ex);
					channelManager.sendErrorMessageAsync(event, "Could not rollback phase change transaction. Please check database for errors: " + ex.getMessage());
				}
			}
		}).start();
	}
}
