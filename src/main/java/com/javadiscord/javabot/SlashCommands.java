package com.javadiscord.javabot;

import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.commands.user_commands.*;
import com.javadiscord.javabot.other.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.ActionRow;
import net.dv8tion.jda.api.interactions.button.Button;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.bson.Document;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class SlashCommands extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getGuild() == null) return;

            switch (event.getName()) {

                case "avatar":

                    OptionMapping option = event.getOption("user");
                    User user = option == null ? event.getUser() : option.getAsUser();

                    Avatar.exCommand(event, user);
                    break;

                case "botinfo":

                    BotInfo.exCommand(event);
                    break;

                case "changemymind":

                    ChangeMyMind.exCommand(event, event.getOption("text").getAsString());
                    break;

                case "help":
                    Help.exCommand(event);
                    break;

                case "idcalc":

                    long idInput = 0;
                    try {
                        idInput = event.getOption("id").getAsLong();
                    } catch (Exception e) {
                        idInput = event.getUser().getIdLong();
                    }

                    IDCalc.exCommand(event, idInput);
                    break;

                case "lmgtfy":

                    Lmgtfy.exCommand(event, event.getOption("text").getAsString());
                    break;

                case "ping":
                    Ping.exCommand(event);
                    break;

                case "profile":

                    OptionMapping profileOption = event.getOption("user");
                    Member member = profileOption == null ? event.getMember() : profileOption.getAsMember();

                    Profile.exCommand(event, member);
                    break;

                case "serverinfo":

                    ServerInfo.exCommand(event);
                    break;

                case "uptime":

                    Uptime.exCommand(event);
                    break;


                default:
                    event.reply("Oops, i can't handle that command right now.").setEphemeral(true).queue();
            }
        }



    @Override
    public void onReady(ReadyEvent event){

        CommandUpdateAction commands = Bot.jda.getGuilds().get(0).updateCommands();

        // Simple reply Commands
        commands.addCommands(
                new CommandData("botinfo", "Shows some information about Java"),
                new CommandData("help", "Sends you a DM with all Commands"),
                new CommandData("ping", "Checks Java's Gateway Ping"),
                new CommandData("serverinfo", "Shows some information about the current guild"),
                new CommandData("uptime", "Checks Java's uptime")
        );

        // Commands with required options
        commands.addCommands(
                new CommandData("avatar", "Shows your profile picture")
                        .addOption(new OptionData(USER, "user", "If given, shows the profile picture of the given user")),

                new CommandData("changemymind", "Generates the \"change my mind\" meme out of your given input")
                        .addOption(new OptionData(STRING, "text", "your text-input").setRequired(true)),

                new CommandData("idcalc", "Generates a human-readable timestamp out of a given id")
                        .addOption(new OptionData(STRING, "id", "The id, that the bot will convert into a human-readable timestamp").setRequired(true)),

                new CommandData("lmgtfy", "Turns your text-input into a lmgtfy-link")
                        .addOption(new OptionData(STRING, "text", "The text, that will be converted into a lmgtfy-link").setRequired(true)),

                new CommandData("profile", "Shows your profile")
                        .addOption(new OptionData(USER, "user", "If given, shows the profile of the given user")));

        commands.queue();
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {

        String[] id = event.getComponentId().split(":");
        String authorId = id[0];
        String type = id[1];

        if (!authorId.equals(event.getUser().getId()) && !(type.startsWith("submission"))) return;

        event.deferEdit().queue();
        switch (type) {
                
            case "submission":

                try {

                    MongoDatabase database = mongoClient.getDatabase("other");
                    MongoCollection<Document> submission_messages = database.getCollection("submission_messages");

                    BasicDBObject submission_criteria = new BasicDBObject()
                            .append("guild_id", event.getGuild().getId())
                            .append("channel_id", event.getChannel().getId())
                            .append("message_id", event.getMessageId());

                    String JSON = submission_messages.find(submission_criteria).first().toJson();

                    JsonObject root = JsonParser.parseString(JSON).getAsJsonObject();
                    String userID = root.get("user_id").getAsString();

                    if (id[2].equals("approve")) {

                        Correct.correct(event, event.getGuild().getMemberById(userID));

                        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                                .setActionRows(ActionRow.of(
                                        Button.success(authorId + ":submission:approve", "Approved by " + event.getMember().getUser().getAsTag()).asDisabled())
                                )
                                .queue();

                    } else if (id[2].equals("decline")) {

                        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                                .setActionRows(ActionRow.of(
                                        Button.danger(authorId + ":submission:decline", "Declined by " + event.getMember().getUser().getAsTag()).asDisabled())
                                )
                                .queue();
                    }

                    submission_messages.deleteOne(submission_criteria);

                } catch (Exception e) { e.printStackTrace(); }
                break;
        }
    }
}


