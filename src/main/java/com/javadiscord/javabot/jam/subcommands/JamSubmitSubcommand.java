package com.javadiscord.javabot.jam.subcommands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.jam.dao.JamSubmissionRepository;
import com.javadiscord.javabot.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.jam.model.Jam;
import com.javadiscord.javabot.jam.model.JamSubmission;
import com.javadiscord.javabot.jam.model.JamTheme;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This command is used to submit a submission for a Java Jam.
 */
public class JamSubmitSubcommand extends ActiveJamSubcommand {
	private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		if (!activeJam.submissionsAllowed()) {
			return Responses.warning(event, "Submissions Not Permitted", "The Jam is not currently accepting submissions.");
		}

		OptionMapping sourceLinkOption = event.getOption("link");
		OptionMapping descriptionOption = event.getOption("description");

		if (sourceLinkOption == null || descriptionOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		String link = sourceLinkOption.getAsString();
		if (!this.validateLink(link)) {
			return Responses.warning(event, "Invalid Source", "The source link you provide must lead to a valid web page.");
		}

		JamSubmission submission = new JamSubmission();
		submission.setUserId(event.getUser().getIdLong());
		submission.setJam(activeJam);
		submission.setThemeName(this.getThemeName(con, activeJam, event));
		submission.setSourceLink(link);
		submission.setDescription(descriptionOption.getAsString());

		new JamSubmissionRepository(con).saveSubmission(submission);
		return Responses.success(event, "Submission Received", "Thank you for your submission to the Jam.");
	}

	/**
	 * Determines if an HTTP link refers to a legitimate web page.
	 * @param link The link to check.
	 * @return True if the link leads to a web page that could be requested, or
	 * false if not.
	 */
	private boolean validateLink(String link) {
		if (!URL_PATTERN.matcher(link).matches()) return false;
		try {
			URL url = new URL(link);
			URLConnection connection = url.openConnection();
			connection.connect();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Determines the name of the Jam theme that the user is making a submission
	 * for. It is only required that the user specify explicitly the theme they
	 * are submitting for, when the jam has more than one active theme.
	 * @param con The database connection.
	 * @param activeJam The jam.
	 * @param event The event which triggered this method.
	 * @return The name of the theme.
	 * @throws SQLException If a database error occurs.
	 */
	private String getThemeName(Connection con, Jam activeJam, SlashCommandEvent event) throws SQLException {
		List<JamTheme> possibleThemes = new JamThemeRepository(con).getAcceptedThemes(activeJam);
		String themeName = null;
		if (possibleThemes.size() > 1) {
			OptionMapping themeOption = event.getOption("theme");
			if (themeOption == null) throw new IllegalArgumentException("This Jam has multiple themes. You must specify the theme you're submitting for.");
			boolean validThemeFound = false;
			for (JamTheme theme : possibleThemes) {
				if (themeOption.getAsString().equals(theme.getName())) {
					themeName = theme.getName();
					validThemeFound = true;
					break;
				}
			}
			if (!validThemeFound) throw new IllegalArgumentException("Couldn't find a theme with the given name.");
		} else if (possibleThemes.size() == 1) {
			themeName = possibleThemes.get(0).getName();
		} else {
			throw new IllegalStateException("Cannot process submissions when there are no accepted themes.");
		}
		if (themeName == null) {
			throw new IllegalStateException("Could not determine the name of the theme to use for the submission.");
		}
		return themeName;
	}
}
