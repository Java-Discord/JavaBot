package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.properties.command.CommandConfig;
import com.javadiscord.javabot.properties.command.CommandDataConfig;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.size;

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
        if (event.getUser().isBot()) return;

        String[] id = event.getComponentId().split(":");
        String type = id[0];

        event.deferEdit().queue();

        switch (type) {
                
            case "submission":

                String authorId = id[1];
                if (!authorId.equals(event.getUser().getId())) return;

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

            case "reactionroles":

                String messageID = id[1];
                String buttonLabel = id[2];
                String emoteID = id[3];

                MongoDatabase database = mongoClient.getDatabase("other");
                MongoCollection<Document> collection = database.getCollection("reactionroles");

                Member member = event.getGuild().retrieveMemberById(event.getUser().getId()).complete();
                Emote emote = event.getGuild().getEmoteById(emoteID);

                BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("message_id", messageID)
                        .append("emote", emote.getAsMention())
                        .append("button_label", buttonLabel);

                String JSON = collection.find(criteria).first().toJson();

                JsonObject Root = JsonParser.parseString(JSON).getAsJsonObject();
                String roleID = Root.get("role_id").getAsString();

                Role role = event.getGuild().getRoleById(roleID);

                if (member.getRoles().contains(role)) event.getGuild().removeRoleFromMember(member, role).queue();
                else event.getGuild().addRoleToMember(member, role).queue();
                break;
        }
    }
}
