package com.javadiscord.javabot.commands.jam.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class Jam {
	private long id;
	private long guildId;
	private LocalDateTime createdAt;
	private long startedBy;
	private LocalDate startsAt;
	private boolean completed;
	private String currentPhase;

	public boolean submissionsAllowed() {
		return this.currentPhase.equals(JamPhase.SUBMISSION);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Jam)) return false;
		Jam jam = (Jam) o;
		return getId() == jam.getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
