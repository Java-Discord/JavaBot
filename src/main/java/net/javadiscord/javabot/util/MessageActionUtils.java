package net.javadiscord.javabot.util;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for message actions.
 */
public class MessageActionUtils {

	private MessageActionUtils() {
	}

	/**
	 * Converts a {@link List} of Message {@link Component}s into a List of actions rows.
	 *
	 * @param components The {@link List} of {@link Component}s.
	 * @return A {@link List} of {@link ActionRow}s.
	 */
	public static List<ActionRow> toActionRows(List<? extends ItemComponent> components) {
		if (components.size() > 25) {
			throw new IllegalArgumentException("Cannot add more than 25 components to a message action.");
		}
		List<ActionRow> rows = new ArrayList<>(5);
		List<ItemComponent> rowComponents = new ArrayList<>(5);
		while (!components.isEmpty()) {
			rowComponents.add(components.remove(0));
			if (rowComponents.size() == 5) {
				rows.add(ActionRow.of(rowComponents));
				rowComponents.clear();
			}
		}
		if (!rowComponents.isEmpty()) {
			rows.add(ActionRow.of(rowComponents));
		}
		return rows;
	}
}
