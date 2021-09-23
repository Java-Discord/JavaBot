package com.javadiscord.javabot.commands.reaction_roles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Misc;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

// TODO: Redo implementation of this to be much cleaner.
@Deprecated(forRemoval = true)
public class ReactionRoles implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        return switch (event.getSubcommandName()) {
            case "create" -> create(event,
                    event.getOption("channel").getAsMessageChannel(),
                    event.getOption("messageid").getAsString(),
                    event.getOption("emote").getAsString(),
                    event.getOption("button-label").getAsString(),
                    event.getOption("role").getAsRole());
            case "delete" -> delete(event,
                    event.getOption("messageid").getAsString(),
                    event.getOption("button-label").getAsString(),
                    event.getOption("emote").getAsString());
            default -> Responses.warning(event, "Unknown subcommand.");
        };
    }

    private ReplyAction create(SlashCommandEvent event, MessageChannel channel, String mID, String emote, String buttonLabel, Role role) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        BasicDBObject criteria = new BasicDBObject()
                .append("guild_id", event.getGuild().getId())
                .append("channel_id", channel.getId())
                .append("message_id", mID)
                .append("button_label", buttonLabel)
                .append("emote", emote);

        if (collection.find(criteria).first() == null) {

            Document doc = new Document()
                    .append("guild_id", event.getGuild().getId())
                    .append("channel_id", channel.getId())
                    .append("message_id", mID)
                    .append("role_id", role.getId())
                    .append("button_label", buttonLabel)
                    .append("emote", emote);

            collection.insertOne(doc);
            var e = new EmbedBuilder()
                    .setTitle("Reaction Role created")
                    .addField("Channel", "<#" + channel.getId() + ">", true)
                    .addField("Role", role.getAsMention(), true)
                    .addField("MessageID", "```" + mID + "```", false)
                    .addField("Emote", "```" + emote + "```", true)
                    .addField("Button Label", "```" + buttonLabel + "```", true)
                    .setColor(Constants.GRAY)
                    .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                    .setTimestamp(new Date().toInstant())
                    .build();


            Misc.sendToLog(event.getGuild(), e);

            channel.retrieveMessageById(mID).queue(this::updateMessageComponents);
            return event.replyEmbeds(e).setEphemeral(true);
        } else return Responses.error(event,
                "A Reaction Role on message `" + mID + "` with emote `" + emote + "` and Button Label `" + buttonLabel + "` already exists.");
    }

    private ReplyAction delete(SlashCommandEvent event, String mID, String buttonLabel, String emote) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        BasicDBObject criteria = new BasicDBObject()
                .append("guild_id", event.getGuild().getId())
                .append("message_id", mID)
                .append("emote", emote)
                .append("button_label", buttonLabel);

        try {
            collection.find(criteria).first().toJson();
        } catch (NullPointerException e) {
            return Responses.error(event,
                    "A Reaction Role on message `" + mID + "` with emote `" + emote + "` and Button Label `" + buttonLabel + "` does not exist.");
        }

        collection.deleteOne(criteria);

        var e = new EmbedBuilder()
                .setTitle("Reaction Role removed")
                .addField("MessageID", "```" + mID + "```", false)
                .addField("Emote", "```" + emote + "```", true)
                .addField("Button Label", "```" + buttonLabel + "```", true)
                .setColor(Constants.GRAY)
                .setFooter(event.getUser().getAsTag(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(new Date().toInstant())
                .build();


        Misc.sendToLog(event.getGuild(), e);

        event.getChannel().retrieveMessageById(mID).queue(this::updateMessageComponents);
        return event.replyEmbeds(e).setEphemeral(true);
    }

    void updateMessageComponents (Message message) {

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("reactionroles");

        List<Button> buttons = new ArrayList<>();
        MongoCursor<Document> it = collection.find(eq("message_id", message.getId())).iterator();

        while (it.hasNext()) {

            JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String label = root.get("button_label").getAsString();
            String emote = root.get("emote").getAsString();
            Emoji emoji = Emoji.fromMarkdown(emote);

            buttons.add(Button.of(ButtonStyle.SECONDARY, "reactionroles:" + message.getId() + ":" + label + ":" + emoji.getId(), label, emoji));
        }

        message.editMessageEmbeds(message.getEmbeds().get(0)).setActionRow(buttons.toArray(new Button[0])).queue();
    }
}
