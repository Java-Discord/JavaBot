package com.javadiscord.javabot.service.jam.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class JamTheme {
	private LocalDateTime createdAt;
	private Jam jam;
	private String name;
	private String description;
	private Boolean accepted;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JamTheme jamTheme)) return false;
		return Objects.equals(getJam(), jamTheme.getJam()) && Objects.equals(getName(), jamTheme.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getJam(), getName());
	}
}
