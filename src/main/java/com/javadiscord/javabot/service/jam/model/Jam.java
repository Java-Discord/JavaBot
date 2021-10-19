package com.javadiscord.javabot.service.jam.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
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
	@Nullable
	private LocalDate endsAt;
	private boolean completed;
	private String currentPhase;

	/**
	 * @return True if the jam currently allows submissions. This is only the
	 * case during the {@link JamPhase#SUBMISSION} phase.
	 */
	public boolean submissionsAllowed() {
		return this.currentPhase.equals(JamPhase.SUBMISSION);
	}

	/**
	 * Gets the name of the jam, or an empty string if the jam doesn't have one.
	 * @return The jam's name, or an empty string.
	 */
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
		if (!(o instanceof Jam jam)) return false;
		return getId() == jam.getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
