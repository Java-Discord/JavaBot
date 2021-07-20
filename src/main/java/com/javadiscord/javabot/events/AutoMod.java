package com.javadiscord.javabot.events;

import com.javadiscord.javabot.commands.moderation.Mute;
import com.javadiscord.javabot.commands.moderation.Warn;
import com.javadiscord.javabot.commands.moderation.actions.MuteAction;
import com.javadiscord.javabot.commands.moderation.actions.WarnAction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoMod extends ListenerAdapter {

    private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)\\/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}\\/)\\w+");

    private String cleanString(String input) {
        input = input.replaceAll("\\p{C}", "");
        input = input.replace(" ", "");
        return input;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        try {
            if (event.getMember().getUser().isBot()) return;


            if (!(event.getMember().hasPermission(Permission.MESSAGE_MANAGE))) {

                if (event.getMessage().getMentionedMembers().size() >= 5) {
//
                    new WarnAction().handle(event, event.getMember(), event.getJDA().getSelfUser(), "Automod: Mention Spam");
                }

                Matcher matcher = inviteURL.matcher(cleanString(event.getMessage().getContentRaw()));

                if (matcher.find()) {
//
                    new WarnAction().handle(event, event.getMember(), event.getJDA().getSelfUser(), "Automod: Advertising");
                    event.getMessage().delete().complete();
                }

                List<Message> history = event.getChannel().getIterableHistory().complete().stream().limit(10).filter(msg -> !msg.equals(event.getMessage())).collect(Collectors.toList());
                int spamCount = history.stream()
                    .filter(message -> message.getAuthor().equals(event.getAuthor()) && !message.getAuthor().isBot())
                    .filter(msg -> (event.getMessage().getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6)
                    .collect(Collectors.toList()).size();


                if (spamCount > 5) {
                    if (!event.getMessage().getAttachments().isEmpty() && event.getMessage().getAttachments().get(0).getFileExtension().equals("java")) return;
//                  
                    new MuteAction().handle(event, event.getMember(), event.getJDA().getSelfUser(), "Automod: Spam");
                }
            }

        } catch (NullPointerException ignored) {}
    }
}

