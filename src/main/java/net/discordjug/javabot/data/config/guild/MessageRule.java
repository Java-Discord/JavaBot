package net.discordjug.javabot.data.config.guild;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.Data;

/**
 * If a message matches all of the given requirements of a rule, the configured action is performed on the message.
 */
@Data
public class MessageRule {
	/**
	 * Messages must match this regex for the rule to activate.
	 */
	private Pattern messageRegex;
	/**
	 * All attachments of the message must match this regex for the rule to activate.
	 */
	private Pattern attachmentNameRegex;
	/**
	 * The number of attachments must be greater than or equal to that field for the rule to activate.
	 */
	private int minAttachments = -1;
	/**
	 * The number of attachments must be less than or equal to that field for the rule to activate.
	 */
	private int maxAttachments = Integer.MAX_VALUE;
	/**
	 * At least one attachment must match at least one of the SHA hashes for the rule to activate.
	 * If this set is empty, this condition is ignored.
	 */
	private Set<String> attachmentSHAs = new HashSet<>();
	
	/**
	 * The action to execute on the message.
	 */
	private MessageAction action = MessageAction.LOG;
	
	/**
	 * Enum for actions that can be performed on messages based on rules.
	 */
	public enum MessageAction {
		/**
		 * The message is logged to a channel.
		 */
		LOG,
		/**
		 * The message is deleted and logged to a channel.
		 */
		BLOCK
	}
}
