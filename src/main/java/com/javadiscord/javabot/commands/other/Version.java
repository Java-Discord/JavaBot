package com.javadiscord.javabot.commands.other;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.javadiscord.javabot.other.Embeds;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Version extends Command {

    public String getVersion () {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        String doc = collection.find(eq("name", "Java#9523")).first().toJson();

        JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
        String version = Root.get("version").getAsString();

        return version;
    }

    public Version () {
        this.name = "version";
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.arguments = "<Text|now>";
        this.help = "list, add or remove reaction roles";
    }

    protected void execute(CommandEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("config");

        String[] args = event.getArgs().split("\\s+");

        String input;
        if (args.length > 0 && args[0].equalsIgnoreCase("now")) {
            input = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM.dd"));
        } else if (args.length > 0) {
            input = args[0];
        } else {
            event.reply(Embeds.syntaxError("version now|Text", event));
            return;
        }

        event.reply(Embeds.configEmbed(event, "Version", "Version succesfully changed to", null, input, true));

        Document Query = new Document();
        Query.append("name", "Java#9523");

        Document SetData = new Document();
        SetData.append("version", input);

        Document update = new Document();
        update.append("$set", SetData);

        collection.updateOne(Query, update);
    }
}