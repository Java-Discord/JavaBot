package net.javadiscord.javabot.systems.jam.subcommands.admin;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.data.config.guild.JamConfig;
import net.javadiscord.javabot.systems.jam.dao.JamRepository;
import net.javadiscord.javabot.systems.jam.model.Jam;
import net.javadiscord.javabot.systems.jam.subcommands.ActiveJamSubcommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Subcommand that allows admins to edit certain properties of the Jam. Uses a
 * mapping of {@link PropertyHandler} instances to handle updates to any
 * eligible property.
 */
public class EditJamSubcommand extends ActiveJamSubcommand {
	private interface PropertyHandler {
		ReplyAction updateProperty(SlashCommandEvent event, Connection con, Jam jam, String value) throws SQLException;
	}

	private static final Map<String, PropertyHandler> propertyHandlers = new HashMap<>();
	static {
		propertyHandlers.put("ends_at", (event, con, jam, value) -> {
			if (value == null) {
				jam.setEndsAt(null);
			} else {
				try {
					LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
					if (!date.isAfter(jam.getStartsAt())) {
						return Responses.warning(event, "End date must be after the start date of " + jam.getStartsAt());
					}
					jam.setEndsAt(date);
				} catch (DateTimeParseException e) {
					return Responses.warning(event, "Invalid date; Expected dd-MM-yyyy format.");
				}
			}
			new JamRepository(con).updateJam(jam);
			return Responses.success(event, "Jam End Date Updated", "The " + jam.getFullName() + " has had its end date updated to " + value);
		});
	}

	@Override
	protected ReplyAction handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con, JamConfig config) throws Exception {
		OptionMapping propertyNameOption = event.getOption("property");
		OptionMapping propertyValueOption = event.getOption("value");
		if (propertyNameOption == null || propertyValueOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		String value = propertyValueOption.getAsString();
		if (value.equalsIgnoreCase("null")) {
			value = null;
		}

		var propertyHandler = propertyHandlers.get(propertyNameOption.getAsString().toLowerCase());
		if (propertyHandler == null) {
			return Responses.warning(event, "Unsupported Property", "Only the following properties may be updated: " + String.join(", ", propertyHandlers.keySet()));
		}
		return propertyHandler.updateProperty(event, con, activeJam, value);
	}
}
