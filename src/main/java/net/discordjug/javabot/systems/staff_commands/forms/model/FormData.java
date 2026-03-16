package net.discordjug.javabot.systems.staff_commands.forms.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.dv8tion.jda.api.components.label.Label;

/**
 * Class containing information about a form.
 * 
 * @param id             the form ID.
 * @param fields         a list of text input fields associated with this form.
 *                       A form can only hold a maximum of 5 fields at a time.
 * @param title          form title used in the modal displayed to the user.
 * @param submitChannel  ID of the channel the form submissions are sent to.
 * @param submitMessage  message displayed to the user once they submit the
 *                       form.
 * @param messageId      ID of the message this form is attached to. null if the
 *                       form is not attached to any message.
 * @param messageChannel channel of the message this form is attached to. null
 *                       if the form is not attached to any message.
 * @param expiration     time after which this user won't accept any further
 *                       submissions. null to indicate that the form has no
 *                       expiration date.
 * @param closed         closed state of this form. If the form is closed, it
 *                       doesn't accept further submissions, even if it's
 *                       expired.
 * @param onetime        onetime state of this form. If it's true, the form only
 *                       accepts one submission per user.
 */
// TODO `Optional` getter for the submit message
public record FormData(long id, List<FormField> fields, String title, long submitChannel, String submitMessage,
		Long messageId, Long messageChannel, Instant expiration, boolean closed, boolean onetime) {

	/**
	 * The main constructor.
	 */
	public FormData {
		Objects.requireNonNull(title);
		fields = List.copyOf(fields);
		if (fields.size() > 5) {
			throw new IllegalArgumentException("fields.size() > 5");
		}
	}

	public Optional<FormAttachmentInfo> getAttachmentInfo() {
		if (messageChannel != null && messageId != null) {
			return Optional.of(new FormAttachmentInfo(messageId, messageChannel));
		}
		return Optional.empty();
	}

	/**
	 * Creates text components for use in the submission modal.
	 *
	 * @return An unmodifiable list of layout components for use in the submission
	 *         modal.
	 */
	public List<Label> createComponents() {
		List<Label> list = new ArrayList<>(fields.size());
		for (int i = 0; i < list.size(); i++) {
			FormField field = fields.get(i);
			list.add(Label.of(fields.get(i).label(), field.createTextInput("text" + i)));
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * Checks if the form can expire.
	 *
	 * @return true if this form has an expiration time.
	 */
	public boolean hasExpirationTime() {
		return expiration != null;
	}

	/**
	 * Checks if the current form still accepts submissions.
	 *
	 * @return true, if the form has expired, false, if the form is still valid or
	 *         can't expire.
	 */
	public boolean hasExpired() {
		return hasExpirationTime() && expiration.isBefore(Instant.now());
	}

	@Override
	public String toString() {
		String prefix;
		if (closed) {
			prefix = "Closed";
		} else if (!hasExpirationTime()) {
			prefix = "Permanent";
		} else if (hasExpired()) {
			prefix = "Expired";
		} else {
			// TODO change how date and time is formatted
			prefix = FormInteractionManager.DATE_FORMAT.format(new Date(expiration.toEpochMilli())) + " UTC";
		}

		return String.format("[%s] %s", prefix, title);
	}

}
