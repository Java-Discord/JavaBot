package net.javadiscord.javabot.systems.commands.staff.custom_commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.events.StartupListener;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;

/**
 * Command that lists Custom Slash Commands.
 */
public class CustomCommandList implements SlashCommandHandler {

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		StringBuilder sb = new StringBuilder();

		for (Document document : StartupListener.mongoClient
				.getDatabase("other")
				.getCollection("customcommands")
				.find(eq("guildId", event.getGuild().getId()))) {

			JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
			String name = root.get("commandName").getAsString();

			sb.append("/" + name);
			sb.append("\n");
		}
		return Responses.success(event,
				"Custom Slash Command List", "```" +
						(sb.length() > 0 ? sb : "No Custom Commands created yet.") + "```"
		);
	}
}
