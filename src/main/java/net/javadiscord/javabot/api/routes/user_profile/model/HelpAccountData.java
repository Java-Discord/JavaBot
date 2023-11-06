package net.javadiscord.javabot.api.routes.user_profile.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.util.ColorUtils;
import net.javadiscord.javabot.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * API-Data class which contains all necessary information about the users'
 * help experience.
 */
@Data
public class HelpAccountData {
	private String currentRank;
	private String currentRankColor;
	private String nextRank;
	private String nextRankColor;
	// Experience
	private double experienceCurrent;
	private double experiencePrevious;
	private double experienceNext;

	/**
	 * A simple utility method which creates an instance of this class based on
	 * the specified {@link HelpAccount}.
	 *
	 * @param botConfig configuration of the bot.
	 * @param account The {@link HelpAccount} to convert.
	 * @param guild   The {@link Guild}.
	 * @return An instance of the {@link HelpAccountData} class.
	 */
	public static @NotNull HelpAccountData of(BotConfig botConfig, @NotNull HelpAccount account, Guild guild) {
		HelpAccountData data = new HelpAccountData();
		data.setExperienceCurrent(account.getExperience());
		Pair<Role, Double> previousRank = account.getPreviousExperienceGoal(botConfig, guild);
		if (previousRank != null && previousRank.first() != null) {
			data.setCurrentRank(previousRank.first().getName());
			data.setCurrentRankColor(ColorUtils.toString(previousRank.first().getColor()));
			data.setExperiencePrevious(previousRank.second());
		}
		Pair<Role, Double> nextRank = account.getNextExperienceGoal(botConfig, guild);
		if (nextRank != null && nextRank.first() != null) {
			data.setNextRank(nextRank.first().getName());
			data.setNextRankColor(ColorUtils.toString(nextRank.first().getColor()));
			data.setExperienceNext(nextRank.second());
		}
		return data;
	}
}
