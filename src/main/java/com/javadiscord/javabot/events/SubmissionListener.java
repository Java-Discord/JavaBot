package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Database;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class SubmissionListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> config = database.getCollection("config");

        String message = event.getMessage().getContentDisplay();
        String[] args = message.split("\\s+");

        if (!args[0].startsWith("!")) {

            MongoCursor<Document> it = config.find(eq("dm-qotw", "true")).iterator();
            ArrayList<String> isAvailable = new ArrayList<String>();

            int i = 0;
            Guild guild;

            while (it.hasNext()) {

                JsonObject root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
                String guildID = root.get("guild_id").getAsString();

                if (event.getJDA().getGuilds().contains(event.getJDA().getGuildById(guildID))) {

                    isAvailable.add(guildID);
                    i++;
                }

            }

            if (i > 0) {

                String guildID = Database.getMemberString(event.getAuthor(), "qotw-guild");

                if (isAvailable.contains(guildID)) {
                    guild = event.getJDA().getGuildById(guildID);
                } else {
                    guild = event.getJDA().getGuildById(isAvailable.get(0));
                }

                try {

                    String sCID = Database.getConfigString(guild.getName(), guild.getId(), "submission_cid");
                    TextChannel submissionChannel = guild.getTextChannelById(sCID);

                    EmbedBuilder submissionEb = new EmbedBuilder()
                            .setColor(Constants.GRAY)
                            .setAuthor("Question of the Week | Submission", null, event.getAuthor().getEffectiveAvatarUrl())
                            .setDescription(message)
                            .addField("Current Guild", guild.getName() + " ``(" + guild.getId() + ")``", false)
                            .setFooter("NOTE: spamming submissions may result in a warn")
                            .setTimestamp(new Date().toInstant());

                    event.getChannel().sendMessage(new MessageBuilder().setEmbed(submissionEb.build()).setActionRows(ActionRow.of(
                            Button.success(event.getAuthor().getId() + ":dm-submission:send", "Send Submission"),
                            Button.danger(event.getAuthor().getId() + ":dm-submission:cancel", "Cancel"))).build()).queue();

                    // TODO: Re-implement this without EventWaiter.
                    event.getMessage().reply("Submission is unsupported at this time.").queue();
//                    EventWaiter waiter = new EventWaiter();
//                    event.getJDA().addEventListener(waiter);
//
//                    waiter.waitForEvent(
//                            ButtonClickEvent.class,
//
//                            e -> e.getChannel().getType() == ChannelType.PRIVATE,
//
//                            e -> {
//
//                                String[] id = e.getComponentId().split(":");
//                                String authorId = id[0];
//
//                                if (!authorId.equals(e.getUser().getId())) return;
//
//                                switch (id[2]) {
//
//                                    case "send":
//
//                                        EmbedBuilder submittedEb = new EmbedBuilder()
//                                                .setColor(Constants.GRAY)
//                                                .setAuthor("Submission by " + e.getUser().getAsTag(), null, e.getUser().getEffectiveAvatarUrl())
//                                                .setDescription(message)
//                                                .setFooter("ID: " + e.getUser().getId())
//                                                .setTimestamp(new Date().toInstant());
//
//                                        submissionChannel.sendMessage(new MessageBuilder().setEmbed(submittedEb.build()).setActionRows(ActionRow.of(
//                                                Button.success(e.getUser().getId() + ":submission:approve", "Approve"),
//                                                Button.danger(e.getUser().getId() + ":submission:decline", "Decline"))).build())
//
//                                                .queue(m -> {
//                                                    MongoCollection<Document> submission_messages = database.getCollection("submission_messages");
//
//                                                    Document doc = new Document()
//                                                            .append("guild_id", m.getGuild().getId())
//                                                            .append("channel_id", m.getChannel().getId())
//                                                            .append("message_id", m.getId())
//                                                            .append("user_id", event.getAuthor().getId());
//
//                                                    submission_messages.insertOne(doc);
//                                                });
//
//                                        e.getHook().editOriginalEmbeds(e.getMessage().getEmbeds().get(0))
//                                                .setActionRows(ActionRow.of(
//                                                        Button.success(authorId + ":dm-submission:send", "Submission sent").asDisabled())
//                                                )
//                                                .queue();
//                                        break;
//
//                                    case "cancel":
//
//                                        e.getHook().editOriginalEmbeds(e.getMessage().getEmbeds().get(0))
//                                                .setActionRows(ActionRow.of(
//                                                        Button.danger(authorId + ":dm-submission:cancel", "Process canceled").asDisabled())
//                                                )
//                                                .queue();
//                                        break;
//                                    }});

                } catch (NullPointerException ignored) { }
            }
        }
    }
}

