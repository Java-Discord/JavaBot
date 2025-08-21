package net.discordjug.javabot.listener.filter;

/**
 * This enum describes the result of an {@link MessageFilter} execution and what should be done with the message.
 */
public enum MessageModificationStatus {
	/**
	 * The message representation has been modified requiring the message to be deleted and re-sent.
	 *
	 * Further filters will be executed.
	 */
	MODIFIED,
	/**
	 * The message representation has not been modified and the filter does not require the message to be deleted and re-sent.
	 *
	 * If another filter returns {@link #MODIFIED}, the message is still deleted and re-sent.
	 * Further filters will be executed.
	 */
	NOT_MODIFIED,
	/**
	 * Indicates that no further filters should be executed on the message and that the message should not be deleted and re-sent by the filter handling logic.
	 */
	STOP_PROCESSING
}