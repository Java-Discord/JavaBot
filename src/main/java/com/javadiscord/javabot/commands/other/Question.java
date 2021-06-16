package com.javadiscord.javabot.commands.other;


import com.javadiscord.javabot.other.Embeds;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.awt.*;

public class Question extends Command {


    public Question () {
        this.name = "question";
        this.category = new Category("OTHER");
        this.arguments = "1-15";
        this.help = "displays the given amount of questions in a random order";
    }

    protected void execute(CommandEvent event) {
        if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

        int num = 0;
        String[] args = event.getArgs().split("\\s+");

        try {
            if (!(Integer.parseInt(args[0]) >= 16)) {

                String[] questionArray = {
                        "Question 0-",
                        "Question 1-",
                        "Question 2-",
                        "Question 3-",
                        "Question 4-",
                        "Question 5-",
                        "Question 6-",
                        "Question 7-",
                        "Question 8-",
                        "Question 9-",
                        "Question 10-",
                        "Question 11-",
                        "Question 12-",
                        "Question 13-",
                        "Question 14-",
                        "Question 15-",
                        "Question 16-"
                };

                StringBuilder sb = new StringBuilder();

                for (int i = Integer.parseInt(args[0]); i > 0; i--) {
                    num = (int) ((Math.random() * (questionArray.length - 1) + 1));
                    String text = sb.toString();

                    while (text.contains("Question " + num + "-")) {
                        num = (int) ((Math.random() * (16) + 1));
                    }
                    sb.append("• " + questionArray[num] + "\n");
                }

                String text = sb.toString()
                        .replace("Question 0-", "Explain the keyword ``synchronized``")
                        .replace("Question 1-", "Explain the keyword ``finally``")
                        .replace("Question 2-", "Explain the keyword ``transient``")
                        .replace("Question 3-", "Explain the keyword ``volatile``")
                        .replace("Question 4-", "How do you manage ``dependencies?``")
                        .replace("Question 5-", "How does Java interact with the OS?")
                        .replace("Question 6-", "What differentiates java from native lags like C++ ?")
                        .replace("Question 7-", "What is ``(@)FunctionalInterface?``")
                        .replace("Question 8-", "What is type erasure?")
                        .replace("Question 9-", "What is ``maven``/``gradle``(/``ant``)?")
                        .replace("Question 10-", "What file format is ``.jar``, actually?")
                        .replace("Question 11-", "What is ``serialization`` and ``deserialization``?")
                        .replace("Question 12-", "Can main method be declared final?")
                        .replace("Question 13-", "How can you achieve multiple Inheritance in Java?")
                        .replace("Question 14-", "What is the problem with ``string1==string2 / System.out.println(array);``")
                        .replace("Question 15-", "What is the difference between ``Integer.valueOf`` and ``Integer.parseInt``?")
                        .replace("Question 16-", "Where do I find documentation for the java language?");

                EmbedBuilder eb = new EmbedBuilder()
                        .setTitle("Questions (" + args[0] + ")")
                        .setDescription(text)
                        .setColor(new Color(0x2F3136));
                event.reply(eb.build());

            } else { event.reply(Embeds.syntaxError("question 1-15", event)); }
            } catch (Exception e) { event.reply(Embeds.syntaxError("question 1-15", event)); }
            } else { event.reply(Embeds.permissionError("MESSAGE_MANAGE", event)); }
    }
}
