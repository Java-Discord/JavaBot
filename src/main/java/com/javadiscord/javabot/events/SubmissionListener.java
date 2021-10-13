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
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
     */
    public void dmSubmissionSend(ButtonClickEvent event) {
        Guild guild = preferredGuild;
        var config = Bot.config.get(guild);

        var e = new EmbedBuilder()
                .setColor(config.getSlashCommand().getDefaultColor())
                .setAuthor("Submission by " + event.getUser().getAsTag(), null, event.getUser().getEffectiveAvatarUrl())
                .setDescription(event.getMessage().getEmbeds().get(0).getDescription())
                .setFooter("ID: " + event.getUser().getId())
                .setTimestamp(Instant.now())
                .build();

        config.getQotw().getSubmissionChannel().sendMessageEmbeds(e)
                .setActionRows(
                ActionRow.of(
                Button.success("submission:approve:" + event.getUser().getId(), "Approve"),
                Button.danger("submission:decline:" + event.getUser().getId(), "Decline"),
                Button.secondary("submission:getraw:" + event.getUser().getId(), "Get Raw"),
                Button.secondary("submission:delete:" + event.getUser().getId(), "🗑️")))
                .queue();

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
    public void submissionApprove (ButtonClickEvent event) {
        var userId = event.getMessage().getEmbeds().get(0)
                .getFooter().getText().replace("ID: ", "");
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
     * Gets called when a moderator presses the "Get Raw" button on a submission.
     * <p>
     * Sends a file that contains the Raw Message/Submission content
     * </p>
     * @param event the ButtonClickEvent that is triggered upon use. {@link InteractionListener#onButtonClick(ButtonClickEvent)}
     */
    public void submissionGetRaw (ButtonClickEvent event) {
        var description = event.getMessage().getEmbeds().get(0).getDescription();
        event.getHook()
                .sendFile(new ByteArrayInputStream(description.getBytes(StandardCharsets.UTF_8)), event.getUser().getId() + ".txt")
                .addActionRow(Button.secondary("submission:delete:" + event.getUser().getId(), "🗑️"))
                .queue();
    }

    /**
     * Gets triggered when a user sends a direct message to the bot.
     * <p>
     * If set by the {@link Startup#preferredGuild}, this will accept submissions
     * </p>
     */
    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.getAuthor().isSystem()) return;

        Guild guild = Startup.preferredGuild;
        var config = Bot.config.get(guild);
        if (!config.getQotw().isDmEnabled()) return;

        String content = event.getMessage().getContentDisplay();

        var e = new EmbedBuilder()
                .setColor(config.getSlashCommand().getDefaultColor())
                .setAuthor("Question of the Week | Submission", null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(content)
                .setFooter("NOTE: spamming submissions may result in a warn")
                .setTimestamp(Instant.now());

        event.getChannel().sendMessageEmbeds(e.build()).setActionRows(
                ActionRow.of(
                Button.success("dm-submission:send:" + event.getAuthor().getId(), "Send Submission"),
                Button.danger("dm-submission:cancel:" + event.getAuthor().getId(), "Cancel")))
                .queue();
        }
    }

