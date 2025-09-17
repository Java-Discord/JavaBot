package net.discordjug.javabot.systems.staff_commands.forms.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;

/**
 * Class containing information about a form.
 */
public class FormData {

	/**
	 * Setting {@link FormData#expiration} to this value indicates, that the form
	 * will never expire.
	 */
	public static final long EXPIRATION_PERMANENT = -1;

	private final boolean closed;
	private final long expiration;
	private final List<FormField> fields;
	private final long id;
	private final Long messageId;
	private final Long messageChannel;
	private final boolean onetime;
	private final long submitChannel;
	private final String submitMessage;
	private final String title;

	/**
	 * Main constructor.
	 *
	 * @param id             The id of this form. The id should be equal to
	 *                       timestamp of creation of this form.
	 * @param fields         List of text inputs of this form.
	 * @param title          Form title shown in the submission modal and in various
	 *                       commands.
	 * @param submitChannel  Target channel where the form submissions will be sent.
	 * @param submitMessage  A message presented to the user after they successfully
	 *                       submit the form.
	 * @param messageId      ID of the message this form is attached to. A null
	 *                       value indicates that this form is not attached to any
	 *                       message.
	 * @param messageChannel Channel of the message this form is attached to. A null
	 *                       value indicates that this form is not attached.
	 * @param expiration     Time after which this form will not accept further
	 *                       submissions. Value of
	 *                       {@link FormData#EXPIRATION_PERMANENT} indicates that
	 *                       this form will never expire.
	 * @param closed         Closed state of this form. A closed form doesn't accept
	 *                       further submissions and has its components disabled.
	 * @param onetime        Whether or not this form accepts one submission per
	 *                       user.
	 */
	public FormData(long id, List<FormField> fields, String title, long submitChannel, String submitMessage,
			Long messageId, Long messageChannel, long expiration, boolean closed, boolean onetime) {
		this.id = id;
		this.fields = Objects.requireNonNull(fields);
		this.title = Objects.requireNonNull(title);
		this.submitChannel = submitChannel;
		this.submitMessage = submitMessage;
		this.messageId = messageId;
		this.messageChannel = messageChannel;
		this.expiration = expiration;
		this.closed = closed;
		this.onetime = onetime;
	}

	public boolean isAttached() {
		return messageChannel != null && messageId != null;
	}

	/**
	 * Creates text components for use in the submission modal.
	 *
	 * @return List of layout components for use in the submission modal.
	 */
	public LayoutComponent[] createComponents() {
		LayoutComponent[] array = new LayoutComponent[fields.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = ActionRow.of(fields.get(i).createTextInput("text" + i));
		}
		return array;
	}

	public long getExpiration() {
		return expiration;
	}

	public List<FormField> getFields() {
		return fields == null ? Collections.emptyList() : fields;
	}

	public long getId() {
		return id;
	}

	public Optional<Long> getMessageChannel() {
		return Optional.ofNullable(messageChannel);
	}

	public Optional<Long> getMessageId() {
		return Optional.ofNullable(messageId);
	}

	public long getSubmitChannel() {
		return submitChannel;
	}

	public String getSubmitMessage() {
		return submitMessage;
	}

	public String getTitle() {
		return title;
	}

	/**
	 * Checks if the form can expire.
	 *
	 * @return true if this form has an expiration time.
	 */
	public boolean hasExpirationTime() {
		return expiration > 0;
	}

	/**
	 * Checks if the current form still accepts submissions.
	 *
	 * @return true, if the form has expired, false, if the form is still valid or
	 *         can't expire.
	 */
	public boolean hasExpired() {
		return hasExpirationTime() && expiration < System.currentTimeMillis();
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isOnetime() {
		return onetime;
	}

	@Override
	public String toString() {
		String prefix;
		if (closed) {
			prefix = "Closed";
		} else if (expiration == EXPIRATION_PERMANENT) {
			prefix = "Permanent";
		} else if (expiration < System.currentTimeMillis()) {
			prefix = "Expired";
		} else {
			prefix = FormInteractionManager.DATE_FORMAT.format(new Date(expiration)) + " UTC";
		}

		return String.format("[%s] %s", prefix, title);
	}

}
