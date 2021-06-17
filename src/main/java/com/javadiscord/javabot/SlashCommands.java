package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.Config;
import com.javadiscord.javabot.commands.configuation.WelcomeImage;
import com.javadiscord.javabot.commands.custom_commands.CustomCommands;
import com.javadiscord.javabot.commands.moderation.*;
import com.javadiscord.javabot.commands.other.Question;
import com.javadiscord.javabot.commands.other.qotw.ClearQOTW;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.commands.other.suggestions.Accept;
import com.javadiscord.javabot.commands.other.suggestions.Clear;
import com.javadiscord.javabot.commands.other.suggestions.Decline;
import com.javadiscord.javabot.commands.other.suggestions.Respond;
import com.javadiscord.javabot.commands.reaction_roles.ReactionRoles;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.properties.command.CommandConfig;
import com.javadiscord.javabot.properties.command.CommandDataConfig;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;

public class SlashCommands extends ListenerAdapter {
    /**
     * Maps every command name and alias to an instance of the command, for
     * constant-time lookup.
     */
    private final Map<String, SlashCommandHandler> commandsIndex;

    public SlashCommands() {
        this.commandsIndex = new HashMap<>();
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getGuild() == null) return;

        var command = this.commandsIndex.get(event.getName());
        if (command != null) {
            command.handle(event);
            return;
        }

        String reason;
        boolean bool;
        Member member;

        switch (event.getName()) {
            // MODERATION
            case "purge":

                try {
                    bool = event.getOption("nuke-channel").getAsBoolean();
                } catch (NullPointerException e) {
                    bool = false;
                }

                Purge.execute(event,
                        (int) event.getOption("amount").getAsLong(),
                        bool);
                break;

            case "report":

                try {
                    reason = event.getOption("reason").getAsString();
                } catch (NullPointerException e) {
                    reason = "None";
                }

                Report.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "unban":

                Unban.execute(event,
                        event.getOption("id").getAsString(),
                        event.getUser());
                break;

            case "unmute":

                Unmute.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser());
                break;

            case "warn":

                try {
                    reason = event.getOption("reason").getAsString();

                } catch (NullPointerException e) {
                    reason = "None";
                }

                Warn.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "warns":

                OptionMapping warnsOption = event.getOption("user");
                member = warnsOption == null ? event.getMember() : warnsOption.getAsMember();

                Warns.execute(event, member);
                break;

            case "customcommand":

                switch (event.getSubcommandName()) {

                    case "list":
                        CustomCommands.list(event);
                        break;

                    case "create":
                        CustomCommands.create(event,
                                event.getOption("name").getAsString(),
                                event.getOption("text").getAsString());

                        break;

                    case "edit":
                        CustomCommands.edit(event,
                                event.getOption("name").getAsString(),
                                event.getOption("text").getAsString());

                        break;

                    case "delete":
                        CustomCommands.delete(event,
                                event.getOption("name").getAsString());

                        break;
                }

                break;

            case "reactionrole":

                switch (event.getSubcommandName()) {

                    case "list":
                        ReactionRoles.list(event);
                        break;

                    case "create":
                        ReactionRoles.create(event,
                                event.getOption("channel").getAsMessageChannel(),
                                event.getOption("messageid").getAsString(),
                                event.getOption("emote").getAsString(),
                                event.getOption("role").getAsRole());

                        break;

                    case "delete":
                        ReactionRoles.delete(event,
                                event.getOption("messageid").getAsString(),
                                event.getOption("emote").getAsString());
                        break;
                }

                break;

            case "leaderboard":

                try {
                    bool = event.getOption("old").getAsBoolean();
                } catch (NullPointerException e) {
                    bool = false;
                }

                Leaderboard.execute(event, bool);
                break;

            case "question":

                Question.execute(event, (int) event.getOption("amount").getAsLong());
                break;

            case "accept":

                Accept.execute(event, event.getOption("message-id").getAsString());
                break;

            case "clear":

                Clear.execute(event, event.getOption("message-id").getAsString());
                break;

            case "decline":

                Decline.execute(event, event.getOption("message-id").getAsString());
                break;

            case "respond":

                Respond.execute(event, event.getOption("message-id").getAsString(), event.getOption("text").getAsString());
                break;

            default:

                try {

                    BasicDBObject criteria = new BasicDBObject()
                            .append("guild_id", event.getGuild().getId())
                            .append("commandname", event.getName());

                    MongoDatabase database = mongoClient.getDatabase("other");
                    MongoCollection<Document> collection = database.getCollection("customcommands");
                    
                    Document it = collection.find(criteria).first();

                    JsonObject Root = JsonParser.parseString(it.toJson()).getAsJsonObject();
                    String value = Root.get("value").getAsString();

                    event.replyEmbeds(new EmbedBuilder().setColor(Constants.GRAY).setDescription(value).build()).queue();

                } catch (Exception e) {
                    event.reply("Oops, this command isnt registered, yet").queue();
                }
        }
    }

    public void registerSlashCommands(Guild guild) {
        CommandListUpdateAction commands = guild.updateCommands();

        CommandConfig[] commandConfigs = CommandDataConfig.load();
        for (CommandConfig config : commandConfigs) {
            if (config.getHandler() != null && !config.getHandler().isEmpty()) {
                try {
                    Class<?> handlerClass = Class.forName(config.getHandler());
                    this.commandsIndex.put(config.getName(), (SlashCommandHandler) handlerClass.getConstructor().newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("Warning: Command \"" + config.getName() + "\" does not have an associated handler class. It will be ignored.");
            }
            commands.addCommands(config.toData());
        }

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");
        MongoCursor<Document> it = collection.find(eq("guild_id", guild.getId())).iterator();

        while (it.hasNext()) {

            JsonObject Root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String commandName = Root.get("commandname").getAsString();
            String value = Root.get("value").getAsString();

            if (value.length() > 100) value = value.substring(0, 97) + "...";
            commands.addCommands(new CommandData(commandName, value));
        }

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
