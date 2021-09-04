package com.javadiscord.javabot.commands.moderation;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * This command deletes messages from a channel.
 */
public class Purge implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return Responses.warning(event, "This command can only be used in a guild.");
        }
        if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
            return Responses.warning(event, "You do not have the `MESSAGE_MANAGE` permission which is required to remove messages.");
        }

        OptionMapping amountOption = event.getOption("amount");
        OptionMapping userOption = event.getOption("user");
        OptionMapping archiveOption = event.getOption("archive");

        Long amount = (amountOption == null) ? null : amountOption.getAsLong();
        User user = (userOption == null) ? null : userOption.getAsUser();
        boolean archive = archiveOption != null && archiveOption.getAsBoolean();

        if (amount != null && (amount < 1 || amount > 1_000_000)) {
            return Responses.warning(event, "Invalid amount. If specified, should be between 1 and 1,000,000, inclusive.");
        }

        TextChannel logChannel = Bot.config.get(event.getGuild()).getModeration().getLogChannel();
        Bot.asyncPool.submit(() -> this.purge(amount, user, archive, event.getTextChannel(), logChannel));
        StringBuilder sb = new StringBuilder();
        sb.append(amount != null ? (amount > 1 ? "Up to " + amount + " messages " : "1 message ") : "All messages ");
        if (user != null) {
            sb.append("by the user ").append(user.getAsTag()).append(' ');
        }
        sb.append("will be removed").append(archive ? " and placed in an archive." : '.');
        return Responses.info(event, "Purge Started", sb.toString());
    }

    private void purge(@Nullable Long amount, @Nullable User user, boolean archive, TextChannel channel, TextChannel logChannel) {
        MessageHistory history = channel.getHistory();
        PrintWriter archiveWriter = null;
        if (archive) {
            try {
                Path purgeArchivesDir = Path.of("purgeArchives");
                if (Files.notExists(purgeArchivesDir)) Files.createDirectory(purgeArchivesDir);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                Path archiveFile = purgeArchivesDir.resolve("purge_" + channel.getName() + "_" + timestamp + ".txt");
                archiveWriter = new PrintWriter(Files.newBufferedWriter(archiveFile), true);
                archiveWriter.println("Purge of channel " + channel.getName());
            } catch (IOException e) {
                logChannel.sendMessage("Could not create archive file for purge of channel " + channel.getAsMention() + ".").queue();
            }
        }
        List<Message> messages;
        long count = 0;
        do {
            messages = history.retrievePast(amount == null ? 100 : (int) Math.min(100, amount)).complete();
            if (!messages.isEmpty()) {
                List<Message> messagesToRemove = new ArrayList<>(100);
                for (Message msg : messages) {
                    // Skip messages which are not from the specified user.
                    if (user != null && !msg.getAuthor().equals(user)) continue;
                    messagesToRemove.add(msg);
                    if (archiveWriter != null) {
                        archiveWriter.printf(
                                "Removing message by %s which was sent at %s\n--- Text ---\n%s\n--- End Text ---\n",
                                msg.getAuthor().getAsTag(),
                                msg.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                msg.getContentRaw()
                        );
                    }
                }
                if (messagesToRemove.size() == 1) {
                    channel.deleteMessageById(messagesToRemove.get(0).getIdLong()).complete();
                } else if (messagesToRemove.size() > 1) {
                    channel.deleteMessages(messagesToRemove).complete();
                }
                count += messagesToRemove.size();
                logChannel.sendMessage("Removed " + messagesToRemove.size() + " messages from " + channel.getAsMention()).queue();
            }
        } while (!messages.isEmpty() && (amount == null || amount > count));
        if (archiveWriter != null) {
            archiveWriter.close();
        }
    }
}