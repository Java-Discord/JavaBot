package com.javadiscord.javabot.commands.other;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bson.Document;

import java.util.Random;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class Question implements SlashCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {
        event.deferReply(false).queue();
        InteractionHook hook = event.getHook();

        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            int num = (int) event.getOption("amount").getAsLong();
            MongoDatabase database = mongoClient.getDatabase("other");
            MongoCollection<Document> collection = database.getCollection("expert_questions");

            long l = collection.count();

            if (!(num > l) && num > 0) {
                Random rand = new Random();
                int i = num;

                StringBuilder sb = new StringBuilder();
                while (i > 0) {

                    int number = rand.nextInt(((int) l - 1) + 1) + 1;
                    String JSON = collection.find(eq("number", number)).first().toJson();
                    JsonObject root = JsonParser.parseString(JSON).getAsJsonObject();

                    String text = root.get("text").getAsString();
                    if (!(sb.toString().contains(text))) {

                        sb.append("• " + text + "\n");
                        i--;
                    } else {
                        continue;
                    }
                }

                var e = new EmbedBuilder()
                    .setColor(Constants.GRAY)
                    .setAuthor("Questions (" + num + ")")
                    .setDescription(sb.toString())
                    .build();

                hook.sendMessageEmbeds(e).queue();

            } else {
                hook.sendMessageEmbeds(Embeds.emptyError("```Please choose a Number between 1 and " + l + "```", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
            }
        } else {
            hook.sendMessageEmbeds(Embeds.permissionError("MESSAGE_MANAGE", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
        }
    }
}
