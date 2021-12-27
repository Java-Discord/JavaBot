package net.javadiscord.javabot.systems.commands.staff.custom_commands.subcommands;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.events.StartupListener;
import net.javadiscord.javabot.systems.commands.staff.custom_commands.CustomCommandHandler;
import org.bson.Document;

import java.time.Instant;

/**
 * Subcommand that allows to create Custom Slash Commands. {@link CustomCommandHandler#CustomCommandHandler()}
 */
public class CustomCommandCreate implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		OptionMapping replyOption = event.getOption("reply");
		boolean reply = replyOption == null || replyOption.getAsBoolean();

		OptionMapping embedOption = event.getOption("embed");
		boolean embed = embedOption == null || embedOption.getAsBoolean();

		OptionMapping nameOption = event.getOption("name");
		OptionMapping textOption = event.getOption("text");

		if (nameOption == null || textOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}

		String name = nameOption.getAsString();
		String text = textOption.getAsString();

		MongoCollection<Document> collection = StartupListener.mongoClient
				.getDatabase("other")
				.getCollection("customcommands");

		if (CustomCommandHandler.commandExists(event.getGuild().getId(), name)) {
			return Responses.error(event, "A Custom Slash Command called " + "`" + "/" + name + "` already exists.");
		}

		collection.insertOne(
				new Document()
						.append("guildId", event.getGuild().getId())
						.append("commandName", name)
						.append("value", text)
						.append("reply", reply)
						.append("embed", embed)
		);

		var e = new EmbedBuilder()
				.setTitle("Custom Command created")
				.addField("Name", "```" + "/" + name + "```", false)
				.addField("Value", "```" + text + "```", false)
				.addField("Reply?", "`" + reply + "`", true)
				.addField("Embed?", "`" + embed + "`", true)
				.setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setTimestamp(Instant.now())
				.build();

		Bot.slashCommands.registerSlashCommands(event.getGuild());
		return event.replyEmbeds(e);
	}
}
