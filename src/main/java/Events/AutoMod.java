package Events;

import Commands.Moderation.Mute;
import Commands.Moderation.Warn;
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
                    Warn.warn(event.getMember(), "Automod: Mention Spam", event.getJDA().getSelfUser().getAsTag(), event);
                }

                Matcher matcher = inviteURL.matcher(cleanString(event.getMessage().getContentRaw()));

                if (matcher.find()) {

                    Warn.warn(event.getMember(), "Automod: Advertising", event.getJDA().getSelfUser().getAsTag(), event);
                    event.getMessage().delete().complete();
                }

                List<Message> history = event.getChannel().getIterableHistory().complete().stream().limit(10).filter(msg -> !msg.equals(event.getMessage())).collect(Collectors.toList());
                int spamCount = history.stream().filter(message -> message.getAuthor().equals(event.getAuthor()) && !message.getAuthor().isBot()).filter(msg -> (event.getMessage().getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).collect(Collectors.toList()).size();

                if (spamCount > 5) {
                    Mute.mute(event.getMember(), event.getJDA().getSelfUser().getAsTag(), event);
                }
            }

        } catch (NullPointerException e) { System.out.println("NullPointerException in AutoMod: GitHub Webhook?");}
    }
}

