package net.javadiscord.javabot.systems.staff_activity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Types of recorded staff activities.
 */
@RequiredArgsConstructor
public enum StaffActivityType {
	/**
	 * The last message sent by the staff member.
	 */
	LAST_MESSAGE("Last message sent");
	
	@Getter
	private final String title;
}
