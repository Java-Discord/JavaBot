package net.javadiscord.javabot.systems.help.model;

import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Pair;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Data class that represents a single Help User Account.
 */
@Data
public class HelpAccount {
	private long userId;
	private double experience;

	public void updateExperience(double change) {
		this.experience += change;
	}

	/**
	 * Tries to get the current experience role.
	 *
	 * @param guild The current {@link Guild}.
	 * @return A {@link Pair} with both the Role, and the experience needed.
	 */
	public Pair<Role, Double> getCurrentExperienceGoal(Guild guild) {
		Map<Long, Double> experienceRoles = Bot.config.get(guild).getHelp().getExperienceRoles();
		Map.Entry<Long, Double> highestExperience = Map.entry(0L, 0.0);
		for (Map.Entry<Long, Double> entry : experienceRoles.entrySet()) {
			if (experience > entry.getValue() && entry.getValue() > highestExperience.getValue()) {
				highestExperience = entry;
			}
		}
		return new Pair<>(guild.getRoleById(highestExperience.getKey()), highestExperience.getValue());
	}

	/**
	 * Tries to get the current, and thus last, experience goal.
	 *
	 * @param guild The current {@link Guild}.
	 * @return The experience needed for the last role, as a {@link Double}.
	 */
	public double getLastExperienceGoal(Guild guild) {
		Optional<Double> experienceOptional = Bot.config.get(guild).getHelp().getExperienceRoles()
				.values().stream()
				.filter(r -> r <= experience)
				.max(Comparator.naturalOrder());
		return experienceOptional.orElse(0.0);
	}

	/**
	 * Tries to get the next experience goal based on the current experience count.
	 *
	 * @param guild The current {@link Guild}.
	 * @return An {@link java.util.Map.Entry} that has the role's id as its key, and the experience needed as its value.
	 */
	public Pair<Role, Double> getNextExperienceGoal(Guild guild) {
		Map<Long, Double> experienceRoles = Bot.config.get(guild).getHelp().getExperienceRoles();
		Optional<Map.Entry<Long, Double>> experienceOptional = experienceRoles.entrySet()
				.stream()
				.filter(r -> r.getValue() > experience)
				.findFirst();
		Map.Entry<Long, Double> entry = experienceOptional.orElseGet(() ->
				Map.entry(experienceRoles.keySet().stream().max(Comparator.naturalOrder()).orElse(0L), 0.0)
		);
		return new Pair<>(guild.getRoleById(entry.getKey()), entry.getValue());
	}
}
