package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.bson.Document;

import java.awt.*;
import java.util.Date;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

public class SubmissionListener extends ListenerAdapter {

    public void dmSubmissionSend (ButtonClickEvent event, String text) {

        Guild guild = preferredGuild;

        MongoDatabase database = mongoClient.getDatabase("other");

        var eb = new EmbedBuilder()
                .setColor(Color.decode(
                        Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor()))
                .setAuthor("Submission by " + event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
                .setDescription(text)
                .setFooter("ID: " + event.getUser().getId())
                .setTimestamp(new Date().toInstant())
                .build();

        Bot.config.get(guild).getQotw().getSubmissionChannel().sendMessageEmbeds(eb).setActionRows(ActionRow.of(
                Button.success("submission:approve:" + event.getUser().getId(), "Approve"),
                Button.danger("submission:decline:" + event.getUser().getId(), "Decline"),
                Button.secondary("submission:delete:" + event.getUser().getId(), "🗑️")))
                .queue(m -> {

                    MongoCollection<Document> submission_messages = database.getCollection("submission_messages");

                    Document doc = new Document()
                            .append("guild_id", m.getGuild().getId())
                            .append("channel_id", m.getChannel().getId())
                            .append("message_id", m.getId())
                            .append("user_id", event.getUser().getId());

                    submission_messages.insertOne(doc);
                });

        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                .setActionRows(ActionRow.of(
                        Button.success("dm-submission:send:" + event.getUser().getId(), "Submission sent").asDisabled()))
                .queue();
    }

    public void dmSubmissionCancel (ButtonClickEvent event) {

        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                .setActionRows(ActionRow.of(
                        Button.danger("dm-submission:cancel:" + event.getUser().getId(), "Process canceled").asDisabled())
                ).queue();
    }

    public void submissionApprove (ButtonClickEvent event, String userID) {

        new Correct().correct(event.getGuild(), event.getGuild().retrieveMemberById(userID).complete());

        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                .setActionRows(ActionRow.of(
                        Button.success("submission:approve:" + userID, "Approved by " + event.getMember().getUser().getAsTag()).asDisabled())
                ).queue();
    }

    public void submissionDecline (ButtonClickEvent event) {

        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                .setActionRows(ActionRow.of(
                        Button.danger("submission:decline:" + event.getUser().getId(), "Declined by " + event.getMember().getUser().getAsTag()).asDisabled())
                )
                .queue();
    }

    public void submissionDelete (ButtonClickEvent event) {

        event.getHook().deleteOriginal().queue();
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentDisplay();
        Guild guild = Startup.preferredGuild;

            if (!Bot.config.get(guild).getQotw().isDmEnabled()) return;
                try {
                    EmbedBuilder submissionEb = new EmbedBuilder()
                            .setColor(Color.decode(
                                    Bot.config.get(event.getAuthor().getMutualGuilds().get(0)).getSlashCommand().getDefaultColor()))
                            .setAuthor("Question of the Week | Submission", null, event.getAuthor().getEffectiveAvatarUrl())
                            .setDescription(message)
                            .addField("Current Guild", guild.getName() + " `(" + guild.getId() + ")`", false)
                            .setFooter("NOTE: spamming submissions may result in a warn")
                            .setTimestamp(new Date().toInstant());

                    MongoDatabase database = mongoClient.getDatabase("other");
                    MongoCollection<Document> collection = database.getCollection("open_submissions");

                    Document document = collection.find(eq("guild_id", guild.getId())).first();

                    if (!(document == null)) {
                        JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
                        String messageId = root.get("message_id").getAsString();
                        Message msg = event.getAuthor().openPrivateChannel().complete().retrieveMessageById(messageId).complete();

                        msg.editMessageEmbeds(msg.getEmbeds().get(0))
                                .setActionRows(ActionRow.of(
                                        Button.danger("dm-submission:canceled:" + event.getAuthor().getId(), "Process canceled").asDisabled()))
                                .queue();
                        collection.deleteOne(document);
                    }

                    event.getChannel().sendMessageEmbeds(submissionEb.build()).setActionRows(ActionRow.of(
                            Button.success("dm-submission:send:" + event.getAuthor().getId(), "Send Submission"),
                            Button.danger("dm-submission:cancel:" + event.getAuthor().getId(), "Cancel"))).queue(
                                m -> {
                                    Document doc = new Document()
                                            .append("guild_id", guild.getId())
                                            .append("message_id", m.getId())
                                            .append("user_id", event.getAuthor().getId())
                                            .append("text", message);

                                    collection.insertOne(doc);
                                });
                } catch (NullPointerException e) { e.printStackTrace(); }
            }
    }

