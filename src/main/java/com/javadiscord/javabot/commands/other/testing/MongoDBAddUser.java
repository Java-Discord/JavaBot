package com.javadiscord.javabot.commands.other.testing;

import com.javadiscord.javabot.other.Database;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Member;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class MongoDBAddUser extends Command {

    public MongoDBAddUser () {
        this.name = "mongoadd";
        this.aliases = new String[]{"add"};
        this.ownerCommand = true;
        this.category = new Category("OWNER");
        this.help = "adds a member to the database";
    }

    protected void execute(CommandEvent event) {

        String[] args = event.getArgs().split("s\\+");
        int QOTWPoints = 0;
        Member member = null;

        try {
            member = event.getGuild().getMemberById(args[0]);
            MongoDatabase database = mongoClient.getDatabase("userdata");

            MongoCollection<Document> collection = database.getCollection("users");

            try {
                String myDoc = collection.find(eq("discord_id", member.getId())).first().toJson();

                JsonObject Root = JsonParser.parseString(myDoc).getAsJsonObject();
                QOTWPoints = Root.get("qotwpoints").getAsInt();

            } catch (NullPointerException e) {
                Document doc = new Document("tag", member.getUser().getAsTag())
                        .append("discord_id", member.getId())
                        .append("warns", 0)
                        .append("qotwpoints", 0);

                collection.insertOne(doc);
            }

            Database.queryMemberInt(event.getGuild().getId(), "qotwpoints", Integer.parseInt(args[1]));
            Database.queryMemberInt(event.getGuild().getId(), "warns", Integer.parseInt(args[2]));

            event.reply("added " + member.getUser().getAsTag() + "! (qotwpoints: " + args[1] + ", warns: " + args[2] + ")");

            } catch (NullPointerException e) {
                event.getChannel().sendMessage("Couldn't find member <:abort:759740784882089995> (" + args[0] + ")").queue();
            }
        }
    }

