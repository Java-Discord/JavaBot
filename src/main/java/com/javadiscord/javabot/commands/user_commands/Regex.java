package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.ResponseException;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.data.properties.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.util.regex.Pattern;

public class Regex implements SlashCommandHandler {

    GuildConfig config;

    @Override
    public ReplyAction handle(SlashCommandEvent event) throws ResponseException {
        config = Bot.config.get(event.getGuild());

        Pattern pattern = Pattern.compile(event.getOption("regex").getAsString());
        String string = event.getOption("string").getAsString();

        return event.replyEmbeds(buildRegexEmbed(pattern.matcher(string).matches(), pattern, string).build());
    }

    private EmbedBuilder buildRegexEmbed(boolean matches, Pattern pattern, String string){
        EmbedBuilder eb = new EmbedBuilder()
                .addField("Regex:", "```" + pattern.toString() + "```", true)
                .addField("String:", "```" + string + "```", true);



        if (matches) {
            eb.setTitle("Regex Tester | ✓ Match");
            eb.setColor(config.getSlashCommand().getSuccessColor());
        } else {
            eb.setTitle("Regex Tester | ✗ No Match");
            eb.setColor(config.getSlashCommand().getErrorColor());
        }

        return eb;
    }

}
