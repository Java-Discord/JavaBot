package net.discordjug.javabot.systems.staff_commands.forms.model;

/**
 * Contains information about form's attachment state. In other words, if the
 * form is attached to a message, this records contains the message's and its
 * channel IDs.
 * 
 * @param messageId        id of the message the form is attached to.
 * @param messageChannelId id of the message's channel
 * 
 * @see FormData
 */
public record FormAttachmentInfo(long messageId, long messageChannelId) {
}
