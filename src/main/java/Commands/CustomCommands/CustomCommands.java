package Commands.CustomCommands;

import Other.Embeds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.bson.Document;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static Events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class CustomCommands extends Command {

    public CustomCommands () {
        this.name = "cc";
        this.aliases = new String[]{"customcommands", "ccmd"};
    }

    protected void execute(CommandEvent event) {

            String[] args = event.getArgs().split("\\s+");
            StringBuilder builder = new StringBuilder();

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("customcommands");

            try {

            if (args[0].equalsIgnoreCase("list")) {

                StringBuilder sb = new StringBuilder();
                MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

                while (it.hasNext()) {

                    JsonObject Root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();

                    String commandName = Root.get("commandname").getAsString();
                    String value = Root.get("value").getAsString();
                    boolean deleteMessage = Root.get("delete_message").getAsBoolean();

                    sb.append("!" + commandName +
                            "\n[size] " + value.length() + " characters" +
                            "\n[delete_message] " + deleteMessage +
                            "\n\n");
                }

                String description;

                if (sb.length() > 0) {
                    description = "```css\n" + sb + "```";
                } else {
                    description = "```No Custom Commands created yet.```";
                }

                if (description.length() >= 2048) {

                    FileWriter fileWriter = new FileWriter("cclist.txt");
                    fileWriter.write(String.valueOf(sb));
                    fileWriter.close();

                    event.getChannel().sendFile(new File("cclist.txt")).queue(); // WIP

                } else {

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Custom Command List")
                        .setDescription(description)
                        .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                        .setColor(new Color(0x2F3136))
                        .setTimestamp(new Date().toInstant());

                event.reply(eb.build());

                }

                return;
            }

            if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                    String commandName = args[1].toLowerCase();
                    if (commandName.startsWith("!")) commandName = commandName.substring(1);

                    BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("commandname", commandName);

                    Document doc = collection.find(criteria).first();

                    switch (args[0]) {

                        case "create":

                            try {

                                if (doc == null) {

                                    String[] Arg = Arrays.copyOfRange(args, 2, args.length);
                                    for (String value : Arg) {
                                        builder.append(value + " ");
                                    }
                                    String text = builder.substring(0, builder.toString().length() - 1);

                                    Document document = new Document()
                                            .append("guild_id", event.getGuild().getId())
                                            .append("commandname", commandName)
                                            .append("value", text)
                                            .append("delete_message", false);

                                    collection.insertOne(document);

                                    EmbedBuilder eb = new EmbedBuilder()
                                            .setTitle("Custom Command created")
                                            .addField("Name", "```" + "!" + commandName + "```", false)
                                            .addField("Value", "```" + text + "```", false)
                                            .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                            .setColor(new Color(0x2F3136))
                                            .setTimestamp(new Date().toInstant());

                                    event.reply(eb.build());

                                } else {
                                    event.reply(Embeds.emptyError("A Custom Command called " + "```" + "!" + commandName + "``` already exists.", event));
                                }

                                } catch (IllegalArgumentException e) {
                                event.reply(Embeds.syntaxError("cc create commandName Text", event));
                            }
                            break;

                        case "delete":

                            try {

                                if (doc == null) {
                                    event.reply(Embeds.emptyError("A Custom Command called " + "```" + "!" + commandName + "``` does not exist.", event));
                                } else {

                                    collection.deleteOne(doc);

                                    EmbedBuilder eb = new EmbedBuilder()
                                            .setTitle("Custom Command deleted")
                                            .addField("Name", "```" + "!" + commandName + "```", false)
                                            .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                            .setColor(new Color(0x2F3136))
                                            .setTimestamp(new Date().toInstant());

                                    event.reply(eb.build());
                                }

                            } catch (ArrayIndexOutOfBoundsException e) {
                                event.reply(Embeds.syntaxError("cc delete commandName", event));
                            }

                            break;

                        case "edit":

                            try {

                                if (doc == null) { event.reply(Embeds.emptyError("A Custom Command called ```" + "!" + commandName + "``` does not exist.", event)); }
                                else {

                                    String[] Arg = Arrays.copyOfRange(args, 2, args.length);
                                    for (String value : Arg) {
                                        builder.append(value + " ");
                                    }
                                    String text = builder.substring(0, builder.toString().length() - 1);

                                    Document setData = new Document();
                                    setData.append("value", text);

                                    Document update = new Document();
                                    update.append("$set", setData);

                                    collection.updateOne(doc, update);

                                    EmbedBuilder eb = new EmbedBuilder()
                                            .setTitle("Custom Command edited")
                                            .addField("Name", "```" + "!" + commandName + "```", false)
                                            .addField("Value", "```" + text + "```", false)
                                            .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                            .setColor(new Color(0x2F3136))
                                            .setTimestamp(new Date().toInstant());

                                    event.reply(eb.build());
                                }

                            } catch (ArrayIndexOutOfBoundsException e) {
                                event.reply(Embeds.syntaxError("cc edit commandName Text", event));
                            }

                            break;

                        case "settings":

                            try {

                                if (doc == null) {
                                    event.reply(Embeds.emptyError("A Custom Command called ```" + "!" + commandName + "``` does not exist.", event));
                                } else {

                                    JsonObject root = JsonParser.parseString(doc.toJson()).getAsJsonObject();
                                    boolean deleteMessage = root.get("delete_message").getAsBoolean();

                                    switch (args[2]) {

                                        case "delete":

                                            boolean input;

                                            if (deleteMessage) {
                                                input = false;
                                            } else {
                                                input = true;
                                            }

                                            Document setData = new Document();
                                            setData.append("delete_message", input);

                                            Document update = new Document();
                                            update.append("$set", setData);

                                            collection.updateOne(doc, update);

                                            EmbedBuilder dMeb = new EmbedBuilder()
                                                    .setTitle("!" + commandName + " | Settings")
                                                    .setDescription("Successfully switched state of ``delete_messages`` to ``" + input + "``")
                                                    .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                                    .setColor(new Color(0x2F3136))
                                                    .setTimestamp(new Date().toInstant());

                                            event.reply(dMeb.build());
                                            break;
                                    }

                                }

                            } catch (ArrayIndexOutOfBoundsException e) {

                                JsonObject root = JsonParser.parseString(doc.toJson()).getAsJsonObject();
                                String value = root.get("value").getAsString();
                                boolean deleteMessage = root.get("delete_message").getAsBoolean();

                                EmbedBuilder sEb = new EmbedBuilder()
                                        .setTitle("!" + commandName + " | Settings")
                                        .setDescription("Delete Command Message: " + "``" + deleteMessage + "``\n" +
                                                "→ ``!cc settings " + commandName + " delete``")
                                        .addField("Value", "```" + value + "```", false)
                                        .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                        .setColor(new Color(0x2F3136))
                                        .setTimestamp(new Date().toInstant());

                                event.reply(sEb.build());
                            }
                            break;
                    }

            } else {
                event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
            }

            } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException | IOException e) {
                e.printStackTrace();
                event.reply(Embeds.syntaxError("cc list|create|delete|edit|settings", event));
            }

        }
    }