package com.javadiscord.javabot.jam.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class Jam {
	private long id;
	private String name;
	private long guildId;
	private LocalDateTime createdAt;
	private long startedBy;
	private LocalDate startsAt;
	private boolean completed;
	private String currentPhase;

	public boolean submissionsAllowed() {
		return this.currentPhase.equals(JamPhase.SUBMISSION);
	}

	public String getNameOrEmpty() {
		if (this.name == null) {
			return "";
		}
		return this.name;
	}

	/**
	 * Gets the full name for the jam, so that it can be used in sentences of
	 * the form "The " + getFullName() + " is starting today!".
	 * @return The full name for the jam, as can be used in a sentence.
	 */
	public String getFullName() {
		if (this.name == null) {
			return "Jam";
		} else {
			return this.name + " Jam";
		}
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
