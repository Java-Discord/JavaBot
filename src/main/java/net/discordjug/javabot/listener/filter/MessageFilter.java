package net.discordjug.javabot.listener.filter;

import org.springframework.core.Ordered;

/**
 * This interface is implemented by all message filters.
 *
 * The {@link MessageContent} is processed by every class implementing {@link MessageFilter}
 * unless one of the filters returns {@link MessageModificationStatus#STOP_PROCESSING} which stops further filters from executing.
 */
public interface MessageFilter extends Ordered {

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

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * By default, message filters have order 0. A higher order means they are executed afterwards while a lower order results in them being executed before.
	 * </p>
	 */
	@Override
	default int getOrder() {
		return 0;
	}
}
