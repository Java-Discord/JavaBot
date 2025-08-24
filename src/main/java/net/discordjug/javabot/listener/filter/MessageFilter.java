package net.discordjug.javabot.listener.filter;

/**
 * This interface is implemented by all message filters.
 *
 * The {@link MessageContent} is processed by every class implementing {@link MessageFilter}
 * unless one of the filters returns {@link MessageModificationStatus#STOP_PROCESSING} which stops further filters from executing.
 */
public interface MessageFilter {

	/**
	 * When a message is received, it is processed by the registered filters.
	 *
	 * @param content The content of the new message that will be reposted instead of the received message
	 * if at least one filter returns {@link MessageModificationStatus#MODIFIED}
	 * and no filter returns {@link MessageModificationStatus#STOP_PROCESSING}.
	 * This {@link MessageContent} is built up incrementally by the filters.
	 * @return the appropriate {@link MessageModificationStatus} based on the filter's processing.
	 * @see MessageFilterHandler
	 */
	MessageModificationStatus processMessage(MessageContent content);

}
