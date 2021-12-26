package net.javadiscord.javabot.systems.moderation;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.moderation.warn.model.WarnSeverity;
import net.javadiscord.javabot.util.Misc;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
public class AutoMod extends ListenerAdapter {

    private static List<String> spamUrls;

    public AutoMod() {
        spamUrls = new ArrayList<>();
        try (var linesStream = Files.lines(Paths.get(getClass().getResource("/spamLinks.txt").toURI()))) {
            linesStream.forEach(spamUrls::add);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Member member = event.getMember();
        if (canBypassAutomod(member)) return;
        checkNewMessageAutomod(event.getMessage());
    }

    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
        Member member = event.getMember();
        if (canBypassAutomod(member)) return;
        checkContentAutomod(event.getMessage());
    }

    /**
     * Checks if a member can bypass the automod system.
     * @param member the {@link Member} to check
     * @return <code>true</code> if the member is allowed to bypass automod, else <code>false</code>
     */
    private boolean canBypassAutomod(Member member) {
        return member == null
                || member.getUser().isBot()
                || member.hasPermission(Permission.MESSAGE_MANAGE);
    }

    /**
     * Runs all automod checks that should be run when a message is sent.
     * @param message the {@link Message} that should be checked
     */
    private void checkNewMessageAutomod(@Nonnull Message message) {
        // mention spam
        if (message.getMentionedMembers().size() >= 5) {
            new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()).getModeration())
                    .warn(
                            message.getMember(),
                            WarnSeverity.MEDIUM,
                            "Automod: Mention Spam",
                            message.getGuild().getMember(message.getJDA().getSelfUser()),
                            message.getTextChannel(),
                            false
                    );
        }

        // spam
        message.getChannel().getHistory().retrievePast(10).queue(messages -> {
            int spamCount = (int) messages.stream().filter(msg -> !msg.equals(message))
                    // filter for spam
                    .filter(msg -> msg.getAuthor().equals(message.getAuthor()) && !msg.getAuthor().isBot())
                    .filter(msg -> (message.getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).count();
            if (spamCount > 5) {
                handleSpam(message, message.getMember());
            }
        });

        checkContentAutomod(message);
    }

    /**
     * Runs all automod checks only depend on the message content.
     * @param message the {@link Message} that should be checked
     */
    private void checkContentAutomod(@Nonnull Message message) {
        // Advertising
        Matcher matcher = inviteURL.matcher(cleanString(message.getContentRaw()));
        if (matcher.find()) {
            new ModerationService(message.getJDA(), Bot.config.get(message.getGuild()).getModeration())
                    .warn(
                            message.getMember(),
                            WarnSeverity.LOW,
                            "Automod: Advertising",
                            message.getGuild().getMember(message.getJDA().getSelfUser()),
                            message.getTextChannel(),
                            false
                    );
        }
        final String messageRaw = message.getContentRaw();
        if (messageRaw.startsWith("http://") || messageRaw.startsWith("https://")) {
            // only do it for a links, so it won't iterate for each message
            for (String spamUrl : spamUrls) {
                if (messageRaw.contains(spamUrl)){
                    try {
                        message.delete().queue();
                        new Warn().warn(message.getMember(), message.getGuild(), "Automod: Suspicious Link");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Handles potential spam messages
     * @param msg the message
     * @param member the member to be potentially warned
     */
    private void handleSpam(@Nonnull Message msg, Member member) {
        // java files -> not spam
        if (!msg.getAttachments().isEmpty()
                && "java".equals(msg.getAttachments().get(0).getFileExtension())) return;

        Role muteRole = Bot.config.get(msg.getGuild()).getModeration().getMuteRole();
        if (member.getRoles().contains(muteRole)) return;

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(msg.getGuild()).getSlashCommand().getErrorColor())
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + msg.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + "Automod: Spam" + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        msg.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(msg.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        // TODO: Replace with Timeout (https://support.discord.com/hc/de/articles/4413305239191-Time-Out-FAQ) once there is a proper JDA Implementation
        new ModerationService(member.getJDA(), Bot.config.get(member.getGuild()).getModeration()).mute(member, member.getGuild());
    }

    /**
     * returns the original String cleaned up of unused code points and spaces
     * @param input the input String
     * @return the cleaned-up String
     */
    private String cleanString(String input) {
        input = input.replaceAll("\\p{C}", "");
        input = input.replace(" ", "");
        return input;
    }
}

