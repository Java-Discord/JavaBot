package net.javadiscord.javabot.systems.expert_questions.model;

import lombok.Data;

@Data
public class ExpertQuestion {
	private long id;
	private long guildId;
	private String text;
}
