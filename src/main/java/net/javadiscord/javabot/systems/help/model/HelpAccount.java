package net.javadiscord.javabot.systems.help.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * Data class that represents a single Help User Account.
 */
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class HelpAccount {
	private long userId;
	private double experience;

	public void updateExperience(double change) {
		this.experience += change;
	}

	/**
	 * Tries to get the current experience role.
	 *
	 * @param botConfig main configuration.
	 * @param guild The current {@link Guild}.
	 * @return A {@link Pair} with both the Role, and the experience needed.
	 */
	public @NotNull Pair<Role, Double> getCurrentExperienceGoal(BotConfig botConfig, Guild guild) {
		Map<Long, Double> experienceRoles = botConfig.get(guild).getHelpConfig().getExperienceRoles();
		Map.Entry<Long, Double> highestExperience = Map.entry(0L, 0.0);
		for (Map.Entry<Long, Double> entry : experienceRoles.entrySet()) {
			if (experience > entry.getValue() && entry.getValue() > highestExperience.getValue()) {
				highestExperience = entry;
			}
		}
		return new Pair<>(guild.getRoleById(highestExperience.getKey()), highestExperience.getValue());
	}

	/**
	 * Tries to get the last experience goal.
	 *
	 * @param botConfig main configuration.
	 * @param guild The current {@link Guild}.
	 * @return The {@link Pair} with both the Role, and the experience needed.
	 */
	public @Nullable Pair<Role, Double> getPreviousExperienceGoal(BotConfig botConfig, Guild guild) {
		Map<Long, Double> experienceRoles = botConfig.get(guild).getHelpConfig().getExperienceRoles();
		Optional<Pair<Role, Double>> experienceOptional = experienceRoles.entrySet().stream()
				.filter(r -> r.getValue() < experience)
				.map(e -> new Pair<>(guild.getRoleById(e.getKey()), e.getValue()))
				.max(Comparator.comparingDouble(Pair::second));
		return experienceOptional.orElse(null);
	}

	/**
	 * Tries to get the next experience goal based on the current experience count.
	 *
	 * @param botConfig main configuration.
	 * @param guild The current {@link Guild}.
	 * @return A {@link Pair} with both the Role, and the experience needed.
	 */
	public @NotNull Pair<Role, Double> getNextExperienceGoal(BotConfig botConfig, Guild guild) {
		Map<Long, Double> experienceRoles = botConfig.get(guild).getHelpConfig().getExperienceRoles();
		Map.Entry<Long, Double> entry = experienceRoles.entrySet()
				.stream()
				.filter(r -> r.getValue() > experience)
				.findFirst().orElseGet(() ->
						Map.entry(experienceRoles.keySet().stream().max(Comparator.naturalOrder()).orElse(0L), 0.0)
				);
		return new Pair<>(guild.getRoleById(entry.getKey()), entry.getValue());
	}
}
