package com.javadiscord.javabot.commands.other;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.bson.Document;

import java.util.List;

import static com.javadiscord.javabot.events.Startup.mongoClient;

public class Question implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        int num = (int) event.getOption("amount").getAsLong();
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("expert_questions");

        long l = collection.countDocuments();
        if (num <= l && num > 0) {
            int i = num;
            StringBuilder sb = new StringBuilder();
            while (i > 0) {
                String json = collection.aggregate(List.of(Aggregates.sample(1))).first().toJson();
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                String text = root.get("text").getAsString();
                if (!(sb.toString().contains(text))) {

                    sb.append("• ").append(text).append("\n");
                    i--;
                }
            }
            var e = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
                .setAuthor("Questions (" + num + ")")
                .setDescription(sb.toString())
                .build();
            return event.replyEmbeds(e);
        } else return Responses.error(event, "```Please choose a Number between 1 and " + l + "```");
    }
}
