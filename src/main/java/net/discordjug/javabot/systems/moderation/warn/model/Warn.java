package net.discordjug.javabot.systems.moderation.warn.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Entity representing an issued warning for a user.
 */
@Data
@NoArgsConstructor
public class Warn {
	private Long id;
	private long userId;
	private long warnedBy;
	private LocalDateTime createdAt;
	private String severity;
	private int severityWeight;
	private String reason;
	private boolean discarded;

	/**
	 * Constructs a new warning.
	 *
	 * @param userId   The id of the user being warned.
	 * @param warnedBy The id of the user who's warning them.
	 * @param severity The severity of the warning.
	 * @param reason   The reason for the warning.
	 */
	public Warn(long userId, long warnedBy, @NotNull WarnSeverity severity, String reason) {
		this.userId = userId;
		this.warnedBy = warnedBy;
		this.severity = severity.name();
		this.severityWeight = severity.getWeight();
		this.reason = reason;
	}
}
