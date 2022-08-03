package net.javadiscord.javabot.api.routes.user_profile.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * API-Data class which contains all necessary information about the users'
 * help experience.
 */
@Data
public class HelpAccountData {
	private String currentRank;
	private Color currentRankColor;
	private String nextRank;
	private Color nextRankColor;
	// Experience
	private double experienceCurrent;
	private double experiencePrevious;
	private double experienceNext;

	/**
	 * A simple utility method which creates an instance of this class based on
	 * the specified {@link HelpAccount}.
	 *
	 * @param account The {@link HelpAccount} to convert.
	 * @param guild   The {@link Guild}.
	 * @return An instance of the {@link HelpAccountData} class.
	 */
	public static @NotNull HelpAccountData of(@NotNull HelpAccount account, Guild guild) {
		HelpAccountData data = new HelpAccountData();
		data.setExperienceCurrent(account.getExperience());
		Pair<Role, Double> previousRank = account.getPreviousExperienceGoal(guild);
		if (previousRank.first() != null) {
			data.setCurrentRank(previousRank.first().getName());
			data.setCurrentRankColor(previousRank.first().getColor());
			data.setExperiencePrevious(previousRank.second());
		}
		Pair<Role, Double> nextRank = account.getNextExperienceGoal(guild);
		if (nextRank.first() != null) {
			data.setNextRank(nextRank.first().getName());
			data.setNextRankColor(nextRank.first().getColor());
			data.setExperienceNext(nextRank.second());
		}
		return data;
	}
}
