package net.javadiscord.javabot.systems.help.model;

import lombok.Data;

/**
 * Data class that represents a single Help User Account.
 */
@Data
public class HelpAccount {
	private long userId;
	private double experience;
	private int helpContributions;

	public void updateExperience(double change) {
		this.experience += change;
	}
}
