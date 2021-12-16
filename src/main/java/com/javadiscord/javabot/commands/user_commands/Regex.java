package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.commands.ResponseException;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.regex.Pattern;

public class Regex implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) throws ResponseException {

        Pattern pattern = Pattern.compile(event.getOption("regex").getAsString());
        String string = event.getOption("string").getAsString();

        if (pattern.matcher(string).matches()){
            return event.replyEmbeds(embedBuilder(true, pattern, string).build());
        } else {
            return event.replyEmbeds(embedBuilder(false, pattern, string).build());
        }
    }

    private EmbedBuilder embedBuilder(boolean matches, Pattern pattern, String string){
        EmbedBuilder eb = new EmbedBuilder()
                .addField("Regex:", "```" + pattern.toString() + "```", true)
                .addField("String:", "```" + string + "```", true);

        if (matches) {
            eb.setTitle("Regex Tester | ✓ Match");
            eb.setColor(0x30bf56);
        } else {
            eb.setTitle("Regex Tester | ✗ No Match");
            eb.setColor(0xd42f47);
        }

        return eb;
    }

}
