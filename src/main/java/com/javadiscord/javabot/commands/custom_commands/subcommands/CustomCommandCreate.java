package com.javadiscord.javabot.commands.custom_commands.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.custom_commands.CustomCommands;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.time.Instant;

import static com.javadiscord.javabot.service.Startup.mongoClient;

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

        MongoCollection<Document> collection = mongoClient
                .getDatabase("other")
                .getCollection("customcommands");

        if (CustomCommands.commandExists(event.getGuild().getId(), name)) {
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
