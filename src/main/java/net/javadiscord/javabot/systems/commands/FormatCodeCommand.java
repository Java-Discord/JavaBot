package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

import java.util.Objects;

public class FormatCodeCommand implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event)  {
        long id;
        try {
            id = Objects.requireNonNull(event.getOption("message-id")).getAsLong();
        } catch (Exception e) {
            if (event.getChannel().hasLatestMessage()) {
                id = event.getChannel().getLatestMessageIdLong();
            } else {
                return Responses.error(event, "Execution failed; no message ID provided and no previous message.");
            }
        }

        Message message;
        try {
            message = event.getChannel().retrieveMessageById(id).complete();
        } catch (Exception exception) {
            return Responses.error(event, exception.getMessage());
        }

        var formatted = "```java\n" + message.getContentRaw() + "```";
        return event.reply(formatted);
    }
}
