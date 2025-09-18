package net.discordjug.javabot.systems.staff_commands.forms.model;

import java.time.Instant;
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
// TODO `Optional` getter for the submit message
public record FormData(long id, List<FormField> fields, String title, long submitChannel, String submitMessage,
		Long messageId, Long messageChannel, Instant expiration, boolean closed, boolean onetime) {

	/**
	 * Setting {@link FormData#expiration} to this value indicates, that the form
	 * will never expire.
	 */
	public static final long EXPIRATION_PERMANENT = -1;

	public FormData {
		Objects.requireNonNull(title);
		fields = List.copyOf(fields);
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

	public Optional<Long> getMessageId() {
		return Optional.ofNullable(messageId);
	}

	public Optional<Long> getMessageChannel() {
		return Optional.ofNullable(messageChannel);
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
