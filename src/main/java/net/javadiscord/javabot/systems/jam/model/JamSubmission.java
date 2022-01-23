package net.javadiscord.javabot.systems.jam.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Simple data class that represents a single Jam Submission.
 */
@Data
public class JamSubmission {
	private long id;
	private LocalDateTime createdAt;
	private Jam jam;
	private String themeName;
	private long userId;
	private String sourceLink;
	private String description;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JamSubmission that)) return false;
		return getId() == that.getId();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}
}
