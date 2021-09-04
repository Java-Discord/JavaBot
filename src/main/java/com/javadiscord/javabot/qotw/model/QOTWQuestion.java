package com.javadiscord.javabot.qotw.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single QOTW question.
 */
@Data
@NoArgsConstructor
public class QOTWQuestion implements Comparable<QOTWQuestion> {
	private long id;
	private LocalDateTime createdAt;
	private long guildId;
	private long createdBy;
	private String text;
	private boolean used;
	private int priority;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof QOTWQuestion that)) return false;
		if (this.getId() == that.getId()) return true;
		return this.getText().equals(that.getText()) && this.getGuildId() == that.getGuildId() && this.getCreatedBy() == that.getCreatedBy() && this.getPriority() == that.getPriority() && this.isUsed() == that.isUsed();
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getCreatedAt(), getGuildId(), getCreatedBy(), getText(), isUsed(), getPriority());
	}

	@Override
	public int compareTo(QOTWQuestion o) {
		int result = Integer.compare(this.getPriority(), o.getPriority());
		if (result == 0) {
			return this.getCreatedAt().compareTo(o.getCreatedAt());
		}
		return result;
	}
}
