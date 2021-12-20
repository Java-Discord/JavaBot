package net.javadiscord.javabot.systems.staff.custom_commands.subcommands;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;
import net.javadiscord.javabot.events.StartupListener;
import net.javadiscord.javabot.systems.staff.custom_commands.CustomCommandHandler;
import org.bson.Document;

import java.time.Instant;

/**
 * Subcommand that allows to delete Custom Slash Commands. {@link CustomCommandHandler#CustomCommandHandler()}
 */
public class CustomCommandDelete implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        OptionMapping nameOption = event.getOption("name");

        if (nameOption == null) {
            return Responses.error(event, "Missing required arguments.");
        }
        String name = nameOption.getAsString();

        MongoCollection<Document> collection = StartupListener.mongoClient
                .getDatabase("other")
                .getCollection("customcommands");

        if (!CustomCommandHandler.commandExists(event.getGuild().getId(), name)) {
            return Responses.error(event, "A Custom Slash Command called `" + "/" + name + "` does not exist.");
        }

       collection.deleteOne(
                new BasicDBObject()
                .append("guildId", event.getGuild().getId())
                .append("commandName", name)
        );

        var e = new EmbedBuilder()
                .setTitle("Custom Slash Command deleted")
                .addField("Name", "```" + "/" + name + "```", false)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setTimestamp(Instant.now())
                .build();

        Bot.slashCommands.registerSlashCommands(event.getGuild());
        return event.replyEmbeds(e);
    }
}
