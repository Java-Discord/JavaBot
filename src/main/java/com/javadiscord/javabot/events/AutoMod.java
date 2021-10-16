package com.javadiscord.javabot.events;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.moderation.Ban;
import com.javadiscord.javabot.commands.moderation.Mute;
import com.javadiscord.javabot.commands.moderation.Warn;
import com.javadiscord.javabot.other.Misc;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
public class AutoMod extends ListenerAdapter {

    private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getMember() == null
                || event.getMember().getUser().isBot()
                || event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) return;


        Member member = event.getMember();

        // mention spam
        if (event.getMessage().getMentionedMembers().size() >= 5) {
            warn(event, member, "Automod: Mention Spam");
        }

        // Advertising
        Matcher matcher = inviteURL.matcher(cleanString(event.getMessage().getContentRaw()));
        if (matcher.find()) {
            warn(event, member, "Automod: Advertising");
        }

        // spam
        event.getChannel().getHistory().retrievePast(10).queue(messages -> {
            int spamCount = (int) messages.stream().filter(msg -> !msg.equals(event.getMessage()))
                    // filter for spam
                    .filter(message -> message.getAuthor().equals(event.getAuthor()) && !message.getAuthor().isBot())
                    .filter(msg -> (event.getMessage().getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).count();
            if (spamCount > 5) {
                handleSpam(event, member);
            }
        });

        // known phishing link
        if (event.getMessage().getContentRaw().contains("https://discordc.gift/")){
            ban(event, member, "scam");
        }
    }

    /**
     * Handles potential spam messages
     * @param event the message event
     * @param member the member to be potentially warned
     */
    private void handleSpam(@NotNull GuildMessageReceivedEvent event, Member member) {
        // java files -> not spam
        if (!event.getMessage().getAttachments().isEmpty()
                && "java".equals(event.getMessage().getAttachments().get(0).getFileExtension())) return;

        Role muteRole = Bot.config.get(event.getGuild()).getModeration().getMuteRole();
        if (member.getRoles().contains(muteRole)) return;

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getErrorColor())
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + "Automod: Spam" + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        event.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        // mute
        try {
            new Mute().mute(event.getMember(), event.getGuild());
        } catch (Exception e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }
    }

    /**
     * warns the user using the reason
     * @param event the message event
     * @param member the member to be warned
     * @param reason the reason for the warning
     */
    private void warn (@NotNull GuildMessageReceivedEvent event, Member member, String reason) {
        int warnPoints = new Warn().getWarnCount(member);

        MessageEmbed eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor())
                .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        event.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        try {
            new Warn().warn(event.getMember(), event.getGuild(), reason);
        } catch (Exception e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }

        event.getMessage().delete().queue();
    }

    /**
     * Bans a user for the given reason
     * @param event the event
     * @param member the user to be banned
     * @param reason the reason for the ban
     */
    private void ban (@NotNull GuildMessageReceivedEvent event, Member member, String reason) {
        MessageEmbed eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor())
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        event.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        try {
            new Ban().ban(member,reason);
        } catch (Exception e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }

        event.getMessage().delete().queue();
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

