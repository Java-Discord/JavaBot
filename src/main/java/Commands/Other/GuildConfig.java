package Commands.Other;

import Other.Database;
import Other.Embeds;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import org.bson.Document;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

import static Events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class GuildConfig extends Command {


    public GuildConfig () {
        this.name = "guild";
        this.guildOnly = false;
        this.category = new Category("OTHER");
        this.arguments = "list|set";
        this.help = "lists or sets available guilds";
    }

    protected void execute(CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");

        if (event.isFromType(ChannelType.PRIVATE)) {

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("config");

            switch (args[0]) {

                case "list":

                    MongoCursor<Document> it = collection.find(eq("dm-qotw", "true")).iterator();
                    StringBuilder sb = new StringBuilder();

                    while (it.hasNext()) {

                        JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
                        String guildID = root.get("guild_id").getAsString();

                        try {
                            Guild guild = event.getJDA().getGuildById(guildID);
                            sb.append(guild.getName() + " (" + guild.getId() + ")\n");
                        } catch (Exception ignored) {
                        }
                    }

                    String description;

                    if (sb.length() > 0) {
                        description = "```" + sb + "```";
                    } else {
                        description = "```No Guilds available.```";
                    }

                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Available Guilds")
                            .setDescription(description)
                            .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                            .setColor(new Color(0x2F3136))
                            .setTimestamp(new Date().toInstant());

                    event.reply(eb.build());
                    break;

                case "set":

                    MongoCursor<Document> doc = collection.find(eq("dm-qotw", "true")).iterator();
                    ArrayList<String> isAvailable = new ArrayList<String>();

                    Guild guild = null;

                    while (doc.hasNext()) {

                        JsonObject root = JsonParser.parseString(doc.next().toJson()).getAsJsonObject();
                        String guildID = root.get("guild_id").getAsString();

                        try {
                            guild = event.getJDA().getGuildById(guildID);
                            isAvailable.add(guild.getId());

                        } catch (Exception ignored) { }
                    }

                    if (isAvailable.contains(args[1])) {

                        guild = event.getJDA().getGuildById(args[1]);
                        Database.queryMemberString(event.getAuthor().getId(), "qotw-guild", guild.getId());

                        EmbedBuilder sEb = new EmbedBuilder()
                                .setTitle("Guild updated!")
                                .setDescription("Succesfully updated your guild to ``" + guild.getName() + "``")
                                .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                .setColor(new Color(0x2F3136))
                                .setTimestamp(new Date().toInstant());

                        event.reply(sEb.build());

                        break;

                    } else {
                        event.reply(Embeds.emptyError("```Guild with id \"" + args[1] + "\" is not available or has no valid submission channel set.```", event));
                    }
                }

            } else {
                event.reply(Embeds.emptyError("```This command cannot be used in Guilds.```", event));
        }
    }
}