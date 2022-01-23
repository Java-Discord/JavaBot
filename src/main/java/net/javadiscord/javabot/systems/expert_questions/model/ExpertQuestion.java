package net.javadiscord.javabot.systems.expert_questions.model;

import lombok.Data;

/**
 * Simple data class that represents a single expert question.
 */
@Data
public class ExpertQuestion {
	private long id;
	private long guildId;
	private String text;
}
