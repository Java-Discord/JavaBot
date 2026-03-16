package net.discordjug.javabot.systems.staff_commands.forms.model;

/**
 * Contains information about form's attachment state. In other words, if the
 * form is attached to a message, this records contains the message's and its
 * channel IDs.
 * 
 * @see FormData
 */
public record FormAttachmentInfo(long messageId, long messageChannelId) {
}
