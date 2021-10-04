package com.javadiscord.javabot.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.other.Constants;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Contains methods and events used for the QOTW-Submission system.
 */
@Slf4j
public class SubmissionListener extends ListenerAdapter {

    /**
     * Gets called when the user presses the "Send Submission" button.
     * <p>
     * Sends the submission to the {@link Startup#preferredGuild}
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     * @param text the text of the submission that was sent.
     */
    public void dmSubmissionSend(ButtonClickEvent event, String text) {
        Guild guild = preferredGuild;
        MongoDatabase database = mongoClient.getDatabase("other");

        var config = Bot.config.get(guild);

        var eb = new EmbedBuilder()
                .setColor(Color.decode(config.getSlashCommand().getDefaultColor()))
                .setAuthor("Submission by " + event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
                .setDescription(text)
                .setFooter("ID: " + event.getUser().getId())
                .setTimestamp(new Date().toInstant())
                .build();

        config.getQotw().getSubmissionChannel().sendMessageEmbeds(eb).setActionRows(ActionRow.of(
                Button.success("submission:approve:" + event.getUser().getId(), "Approve"),
                Button.danger("submission:decline:" + event.getUser().getId(), "Decline"),
                Button.secondary("submission:delete:" + event.getUser().getId(), "🗑️")))
                .queue(m -> {
                    MongoCollection<Document> collection = database.getCollection("submission_messages");
                    collection.insertOne(new Document()
                            .append("guild_id", m.getGuild().getId())
                            .append("channel_id", m.getChannel().getId())
                            .append("message_id", m.getId())
                            .append("user_id", event.getUser().getId()));
                });
        event.getHook().editOriginalComponents()
                .setActionRows(ActionRow.of(Button.success("dm-submission:send:" + event.getUser().getId(),
                        "Submission sent").asDisabled())).queue();

        log.info("{}[{}]{} User {} sent a submission.",
                Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET,
                event.getUser().getAsTag());
    }

    /**
     * Gets called when the user presses the "Cancel" button.
     * <p>
     * Cancels the current process.
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     */
    public void dmSubmissionCancel (ButtonClickEvent event) {
        event.getHook().editOriginalComponents()
                .setActionRows(ActionRow.of(Button.danger("dm-submission:cancel:" + event.getUser().getId(),
                        "Process canceled").asDisabled())).queue();
    }

    /**
     * Gets called when a moderator presses the "Approve" button on a submission.
     * <p>
     * Approves the corresponding submission and grants 1 QOTW-Point.
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     */
    public void submissionApprove (ButtonClickEvent event, String userId) {
        var member = event.getGuild().retrieveMemberById(userId).complete();
        new Correct().correct(event.getGuild(), member);
        log.info("{}[{}]{} Submission by User {} was approved by {}",
                Constants.TEXT_WHITE, event.getGuild().getName(), Constants.TEXT_RESET,
                member.getUser().getAsTag(), event.getUser().getAsTag());

        event.getHook().editOriginalComponents()
                .setActionRows(ActionRow.of(Button.success("submission:approve:" + userId,
                        "Approved by " + event.getMember().getUser().getAsTag()).asDisabled())).queue();
    }


    /**
     * Gets called when a moderator presses the "Decline" button on a submission.
     * <p>
     * Declines the corresponding submission.
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     */
    public void submissionDecline (ButtonClickEvent event) {
        event.getHook().editOriginalComponents()
                .setActionRows(ActionRow.of(Button.danger("submission:decline:" + event.getUser().getId(),
                        "Declined by " + event.getMember().getUser().getAsTag()).asDisabled())).queue();
    }

    /**
     * Gets called when a moderator presses the "🗑️" button on a submission.
     * <p>
     * Deletes the submission message.
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     */
    public void submissionDelete (ButtonClickEvent event) {
        event.getHook().deleteOriginal().queue();
    }

    /**
     * Gets triggered when a user sends a direct message to the bot.
     * <p>
     * If set by the {@link Startup#preferredGuild}, this will accept submissions
     * </p>
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentDisplay();
        Guild guild = Startup.preferredGuild;
        var config = Bot.config.get(guild);

        if (!config.getQotw().isDmEnabled()) return;

        var eb = new EmbedBuilder()
                .setColor(Color.decode(config.getSlashCommand().getDefaultColor()))
                .setAuthor("Question of the Week | Submission", null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(message)
                .setFooter("NOTE: spamming submissions may result in a warn")
                .setTimestamp(new Date().toInstant());

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("open_submissions");

        Document document = collection.find(eq("guild_id", guild.getId())).first();

        if (document != null) {
            JsonObject root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String messageId = root.get("message_id").getAsString();
            Message msg = event.getAuthor().openPrivateChannel().complete().retrieveMessageById(messageId).complete();

            msg.editMessageComponents()
                    .setActionRows(ActionRow.of(Button.danger("dm-submission:canceled:" + event.getAuthor().getId(),
                                    "Process canceled").asDisabled())).queue();
            collection.deleteOne(document);
        }
        event.getChannel().sendMessageEmbeds(eb.build()).setActionRows(ActionRow.of(
                Button.success("dm-submission:send:" + event.getAuthor().getId(), "Send Submission"),
                Button.danger("dm-submission:cancel:" + event.getAuthor().getId(), "Cancel"))).queue(
                m -> collection.insertOne(new Document()
                        .append("guild_id", guild.getId())
                        .append("message_id", m.getId())
                        .append("user_id", event.getAuthor().getId())
                        .append("text", message)));
        }
    }

