package com.javadiscord.javabot.commands.custom_commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.awt.*;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

// TODO: Remove this class, use DelegatingCommandHandler or something to make it readable maybe.
@Deprecated(forRemoval = true)
public class CustomCommands implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        switch (event.getSubcommandName()) {
            case "create":
                return create(event,
                    event.getOption("name").getAsString(),
                    event.getOption("text").getAsString());
            case "edit":
                return edit(event,
                    event.getOption("name").getAsString(),
                    event.getOption("text").getAsString());
            case "delete":
                return delete(event,
                    event.getOption("name").getAsString());
        }
        return Responses.warning(event, "Unknown subcommand.");
    }

    public static boolean docExists(String guildID, String commandName) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");

        BasicDBObject criteria = new BasicDBObject()
            .append("guild_id", guildID)
            .append("commandname", commandName);

        Document doc = collection.find(criteria).first();

        return doc == null;
    }

    public static void list(SlashCommandEvent event) {

        StringBuilder sb = new StringBuilder();
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");
        MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

        while (it.hasNext()) {

            JsonObject Root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String commandName = Root.get("commandname").getAsString();

            sb.append("/" + commandName + "\n");
        }

        String description;

        if (sb.length() > 0) description = "```css\n" + sb + "```";
        else description = "```No Custom Commands created yet.```";

        var e = new EmbedBuilder()
            .setTitle("Custom Slash Command List")
            .setDescription(description)
            .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
            .setColor(new Color(0x2F3136))
            .setTimestamp(new Date().toInstant())
            .build();

        event.replyEmbeds(e).queue();
    }

    private ReplyAction create(SlashCommandEvent event, String commandName, String value) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("customcommands");

            if (docExists(event.getGuild().getId(), commandName)) {

                Document document = new Document()
                    .append("guild_id", event.getGuild().getId())
                    .append("commandname", commandName)
                    .append("value", value);

                collection.insertOne(document);

                var e = new EmbedBuilder()
                    .setTitle("Custom Command created")
                    .addField("Name", "```" + "/" + commandName + "```", false)
                    .addField("Value", "```" + value + "```", false)
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setColor(new Color(0x2F3136))
                    .setTimestamp(new Date().toInstant())
                    .build();

                Bot.slashCommands.registerSlashCommands(event.getGuild());
                return event.replyEmbeds(e);
            } else {
                return event.replyEmbeds(Embeds.emptyError("A Custom Slash Command called " + "``" + "/" + commandName + "`` already exists.", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL);
            }
        } else {
            return event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }

    private ReplyAction edit(SlashCommandEvent event, String commandName, String value) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("customcommands");

            if (docExists(event.getGuild().getId(), commandName)) {
                return event.replyEmbeds(Embeds.emptyError("A Custom Slash Command called ```" + "/" + commandName + "``` does not exist.", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL);
            } else {
                BasicDBObject criteria = new BasicDBObject()
                    .append("guild_id", event.getGuild().getId())
                    .append("commandname", commandName);

                Document doc = collection.find(criteria).first();

                Document setData = new Document();
                setData.append("value", value);

                Document update = new Document();
                update.append("$set", setData);

                collection.updateOne(doc, update);

                var e = new EmbedBuilder()
                    .setTitle("Custom Slash Command edited")
                    .addField("Name", "```" + "/" + commandName + "```", false)
                    .addField("Value", "```" + value + "```", false)
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setColor(new Color(0x2F3136))
                    .setTimestamp(new Date().toInstant())
                    .build();

                Bot.slashCommands.registerSlashCommands(event.getGuild());
                return event.replyEmbeds(e);
            }
        } else {
            return event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }

    private ReplyAction delete(SlashCommandEvent event, String commandName) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("customcommands");

            if (docExists(event.getGuild().getId(), commandName)) {
                return event.replyEmbeds(Embeds.emptyError("A Custom Slash Command called ```" + "/" + commandName + "``` does not exist.", event.getUser())).setEphemeral(Constants.ERR_EPHEMERAL);
            } else {

                BasicDBObject criteria = new BasicDBObject()
                    .append("guild_id", event.getGuild().getId())
                    .append("commandname", commandName);

                Document doc = collection.find(criteria).first();

                collection.deleteOne(doc);

                var e = new EmbedBuilder()
                    .setTitle("Custom Slash Command deleted")
                    .addField("Name", "```" + "/" + commandName + "```", false)
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setColor(new Color(0x2F3136))
                    .setTimestamp(new Date().toInstant())
                    .build();

                Bot.slashCommands.registerSlashCommands(event.getGuild());
                return event.replyEmbeds(e);
            }
        } else {
            return event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL);
        }
    }
}