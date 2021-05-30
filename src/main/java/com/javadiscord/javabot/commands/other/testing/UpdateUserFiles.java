package com.javadiscord.javabot.commands.other.testing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class UpdateUserFiles extends Command {

    public UpdateUserFiles() {
        this.name = "updateuf";
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.help = "updates all user files";
    }

    protected void execute(CommandEvent event) {

        MongoDatabase database = mongoClient.getDatabase("userdata");
        MongoCollection<Document> collection = database.getCollection("users");

        MongoCursor<Document> doc = collection.find().iterator();
        int i = 0;
        while (doc.hasNext()) {


            Document document1 = doc.next();
            i++;

            JsonObject Root = JsonParser.parseString(document1.toJson()).getAsJsonObject();
            String tag = Root.get("tag").getAsString();
            String discordID = Root.get("discord_id").getAsString();
            int warns = Root.get("warns").getAsInt();
            int qotwpoints = Root.get("qotwpoints").getAsInt();

            Document SetData = new Document()
                    .append("tag", tag)
                    //.append("discordID", discordID)
                    .append("warns", warns)
                    .append("qotwpoints", qotwpoints)
                    .append("qotw-guild", "648956210850299986");

            Document update = new Document();
            update.append("$set", SetData);

            collection.updateOne(document1, update);
            System.out.println("#" + i + ": Updated File for " + tag + " (" + discordID + ")");
        }
    }
}