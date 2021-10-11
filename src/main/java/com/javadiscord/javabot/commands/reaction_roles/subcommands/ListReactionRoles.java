package com.javadiscord.javabot.commands.reaction_roles.subcommands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.awt.*;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class ListReactionRoles implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        StringBuilder sb = new StringBuilder();
        MongoCursor<Document> it = collection.find(eq("guild_id", event.getGuild().getId())).iterator();

        for (int i = 1; it.hasNext(); i++) {

            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String channelID = root.get("channel_id").getAsString();
            String messageID = root.get("message_id").getAsString();
            String roleID = root.get("role_id").getAsString();
            String emoteName = root.get("emote").getAsString();
            String label = root.get("button_label").getAsString();

            sb.append("#ReactionRole")
                    .append(i)
                    .append("\n[CID] ").append(channelID)
                    .append("\n[MID] ").append(messageID)
                    .append("\n[RID] ").append(roleID)
                    .append("\n[Label] ").append(label)
                    .append("\n[Emote] ").append(emoteName)
                    .append("\n\n");
        }

        String description;

        if (sb.length() > 0) {
            description = "```css\n" + sb + "```";
        } else {
            description = "```No Reaction Roles created yet```";
        }

        var e = new EmbedBuilder()
                .setTitle("Reaction Role List")
                .setDescription(description)
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();

        return event.replyEmbeds(e);
    }
}
