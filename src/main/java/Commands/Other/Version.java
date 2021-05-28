package Commands.Other;

import Other.Embeds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static Events.Startup.mongoClient;
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

        try {

            String input;

            if (args[0].equalsIgnoreCase("now")) {

                LocalDate currentdate = LocalDate.now();
                String currentDay = currentdate.format(DateTimeFormatter.ofPattern("dd"));
                String currentMonth = currentdate.format(DateTimeFormatter.ofPattern("MM"));
                String currentYear = currentdate.format(DateTimeFormatter.ofPattern("YYYY")).substring(2);

                input = currentYear + "-" + currentMonth + "." + currentDay;

            } else {
                input = args[0];
            }

            event.reply(Embeds.configEmbed(event, "Version", "Version succesfully changed to", null, input, true));

            Document Query = new Document();
            Query.append("name", "Java#9523");

            Document SetData = new Document();
            SetData.append("version", input);

            Document update = new Document();
            update.append("$set", SetData);

            collection.updateOne(Query, update);

        } catch (ArrayIndexOutOfBoundsException e) {
            event.reply(Embeds.syntaxError("version now|Text", event));
        }
    }
}