package Commands.ReactionRoles;

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
import java.util.Date;

import static Events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ReactionRoles extends Command {

    public ReactionRoles() {
        this.name = "rr";
        this.aliases = new String[]{"reactionroles"};
        this.category = new Category("MODERATION");
        this.arguments = "list|add|remove";
        this.help = "list, add or remove reaction roles";

    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

            String[] args = event.getArgs().split("\\s+");

            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("reactionroles");

            boolean validEmote = false;
            boolean integratedEmote = false;
            String emoteName = null;
            String channelID, roleID;

            try {

                switch (args[0]) {

                    case "list":

                        StringBuilder sb = new StringBuilder();
                        MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

                        for (int i = 1; it.hasNext(); i++) {

                            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
                            channelID = root.get("channel_id").getAsString();
                            String messageID = root.get("message_id").getAsString();
                            roleID = root.get("role_id").getAsString();
                            emoteName = root.get("emote").getAsString();

                            sb.append("#ReactionRole" + i +
                                    "\n[CID] " + channelID +
                                    "\n[MID] " + messageID +
                                    "\n[RID] " + roleID +
                                    "\n[EmoteName] " + emoteName + "\n\n");
                        }

                        String description;

                        if (sb.length() > 0) {
                            description = "```css\n" + sb + "```";
                        } else {
                            description = "```No Reaction Roles created yet```";
                        }

                        if (description.length() >= 2048) {

                            FileWriter fileWriter = new FileWriter("rrlist.txt");
                            fileWriter.write(String.valueOf(sb));
                            fileWriter.close();

                            event.getChannel().sendFile(new File("rrlist.txt")).queue();

                        } else {

                            EmbedBuilder lEb = new EmbedBuilder()
                                    .setTitle("Reaction Role List")
                                    .setDescription(description)
                                    .setColor(new Color(0x2F3136))
                                    .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                    .setTimestamp(new Date().toInstant());

                            event.reply(lEb.build());
                        }

                        break;

                    case "add":

                        //rr add #Channel/ID MID :emote: @Role/ID
                        //    0       1       2     3        4

                        try {

                            if (!event.getMessage().getMentionedChannels().isEmpty()) {
                                channelID = event.getMessage().getMentionedChannels().get(0).getId();
                            } else {
                                channelID = event.getGuild().getTextChannelById(args[1]).getId();
                            }

                            if (!event.getMessage().getMentionedRoles().isEmpty()) {
                                roleID = event.getMessage().getMentionedRoles().get(0).getId();
                            } else {
                                roleID = event.getGuild().getRoleById(args[4]).getId();
                            }

                            if (args[3].length() < 24) {
                                emoteName = args[3];
                                integratedEmote = true;
                                validEmote = true;
                            }

                            if (!integratedEmote) {
                                emoteName = args[3].substring(2, args[3].length() - 20);

                                if (emoteName.startsWith(":")) emoteName = emoteName.substring(1);

                                try {
                                    if (event.getGuild().getEmotes().contains(event.getGuild().getEmotesByName(emoteName, false).get(0))) {
                                        validEmote = true;

                                    } else {
                                        event.reply(Embeds.emptyError("```Please provide a valid Emote.```", event));
                                        return;
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    event.reply(Embeds.emptyError("```Please provide a valid Emote.```", event)); return;
                                }
                            }

                            BasicDBObject criteria = new BasicDBObject()
                                    .append("guild_id", event.getGuild().getId())
                                    .append("channel_id", channelID)
                                    .append("message_id", args[2])
                                    .append("emote", emoteName);

                            if (collection.find(criteria).first() == null) {

                                if (validEmote) {
                                    if (integratedEmote) { event.getGuild().getTextChannelById(channelID).addReactionById(args[2], emoteName).complete(); }
                                    else { event.getGuild().getTextChannelById(channelID).addReactionById(args[2], event.getGuild().getEmotesByName(emoteName, false).get(0)).complete(); }
                                }

                                Document doc = new Document()
                                        .append("guild_id", event.getGuild().getId())
                                        .append("channel_id", channelID)
                                        .append("message_id", args[2])
                                        .append("role_id", roleID)
                                        .append("emote", emoteName);

                                collection.insertOne(doc);


                                EmbedBuilder cEb = new EmbedBuilder()
                                        .setTitle("Reaction Role created")
                                        .addField("Channel", "<#" + channelID + ">", true)
                                        .addField("Role", "<@&" + roleID + ">", true)
                                        .addField("Emote", "``" + emoteName + "``", true)
                                        .addField("MessageID", "```" + args[2] + "```", false)
                                        .setColor(new Color(0x2F3136))
                                        .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                        .setTimestamp(new Date().toInstant());

                                event.reply(cEb.build());

                            } else {
                                event.reply(Embeds.emptyError("A Reaction Role on message ``" + args[2] + "`` with emote ``" + emoteName + "`` already exists.", event));
                            }

                            } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                            event.reply(Embeds.syntaxError("rr add #Channel/ID MID :emote: @Role/ID", event));
                        }
                        break;

                    case "remove":

                        // rr remove messageID :emote:
                        //      0        1        2

                        try {

                            if (args[2].length() < 24) {
                                emoteName = args[2];
                                integratedEmote = true;
                                validEmote = true;
                            }

                            if (!integratedEmote) {
                                emoteName = args[2].substring(2, args[2].length() - 20);

                                if (emoteName.startsWith(":")) emoteName = emoteName.substring(1);

                                try {
                                    if (event.getGuild().getEmotes().contains(event.getGuild().getEmotesByName(emoteName, false).get(0))) { validEmote = true; }

                                    else { event.reply(Embeds.emptyError("```Please provide a valid Emote.```", event)); return; }
                                    } catch (IndexOutOfBoundsException e) { event.reply(Embeds.emptyError("```Please provide a valid Emote.```", event)); return; }
                            }


                                BasicDBObject criteria = new BasicDBObject()
                                        .append("guild_id", event.getGuild().getId())
                                        .append("message_id", args[1])
                                        .append("emote", emoteName);

                                String doc = collection.find(criteria).first().toJson();
                                JsonObject Root = JsonParser.parseString(doc).getAsJsonObject();
                                channelID = Root.get("channel_id").getAsString();

                                if (validEmote) {
                                    if (integratedEmote) { event.getGuild().getTextChannelById(channelID).removeReactionById(args[1], emoteName).complete(); }
                                    else { event.getGuild().getTextChannelById(channelID).removeReactionById(args[1], event.getGuild().getEmotesByName(emoteName, false).get(0)).complete(); }
                                }

                                collection.deleteOne(criteria);

                                EmbedBuilder dEb = new EmbedBuilder()
                                        .setTitle("Reaction Role removed")
                                        .addField("Emote", "```" + emoteName + "```", true)
                                        .addField("MessageID", "```" + args[1] + "```", true)
                                        .setColor(new Color(0x2F3136))
                                        .setFooter(event.getAuthor().getAsTag(), event.getAuthor().getEffectiveAvatarUrl())
                                        .setTimestamp(new Date().toInstant());

                                event.reply(dEb.build());

                            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                                event.reply(Embeds.syntaxError("rr remove messageID :emote:", event));

                            } catch (NullPointerException e) {
                            event.reply(Embeds.emptyError("No Reaction Role with emote ``" + emoteName + "`` on message ``" + args[1] + "`` was found.", event));
                        }
                        break;

                    default: event.reply(Embeds.syntaxError("rr list|add|remove", event));
                }

                } catch (ArrayIndexOutOfBoundsException | IOException e) {
                    event.reply(Embeds.syntaxError("rr list|add|remove", event));
                }

                } else {
                    event.reply(Embeds.permissionError("MESSAGE_MANAGE", event));
        }
    }
}
