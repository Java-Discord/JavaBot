package net.javadiscord.javabot.systems.staff_activity.model;

/**
 * Represents metadata of a message where activity information of a staff member is stored.
 * @param guildId the ID of the guild
 * @param userId the ID of the staff member
 * @param messageId the ID of the message storing activity information about the staff member
 */
public record StaffActivityMessage(long guildId, long userId, long messageId) {}
