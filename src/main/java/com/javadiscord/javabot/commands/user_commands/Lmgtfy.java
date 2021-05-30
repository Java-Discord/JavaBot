package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Lmgtfy extends Command {

    public static void exCommand (CommandEvent event) {

        String[] args = event.getArgs().split("\\s+");

        try {

            String[] argRange = Arrays.copyOfRange(args, 0, args.length);
            StringBuilder builder = new StringBuilder();
            for (String value : argRange) {
                builder.append(value + " ");
            }
            String searchTerm = builder.substring(0, builder.toString().length() - 1);
            String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString());
            event.reply("<https://lmgtfy.com/?q=" + encodedSearchTerm + ">");

        } catch (Exception e) {
            event.reply(Embeds.syntaxError("lmgtfy SearchTerm", event));
        }
    }

    public static void exCommand (SlashCommandEvent event, String text) {

        String encodedSearchTerm = null;

        try {
             encodedSearchTerm = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) { e.printStackTrace(); }

            event.reply("<https://lmgtfy.com/?q=" + encodedSearchTerm + ">").queue();
    }


    public Lmgtfy() {
        this.name = "lmgtfy";
        this.aliases = new String[]{ "letmegooglethatforyou" };
        this.category = new Category("USER COMMANDS");
        this.arguments = "<Text>";
        this.help = "Turns your input into a lmgtfy-link";
    }

    @Override
    protected void execute(CommandEvent event) {

       exCommand(event);
    }
}