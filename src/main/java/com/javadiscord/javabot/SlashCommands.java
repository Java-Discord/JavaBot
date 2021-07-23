package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.events.SubmissionListener;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.properties.command.CommandConfig;
import com.javadiscord.javabot.properties.command.CommandDataConfig;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.javadiscord.javabot.events.Startup.preferredGuild;
import static com.mongodb.client.model.Filters.eq;

/**
 * This listener is responsible for handling slash commands sent by users in
 * guilds where the bot is active, and responding to them by calling the
 * appropriate {@link SlashCommandHandler}.
 * <p>
 *     The list of valid commands, and their associated handlers, are defined in
 *     the commands.yaml file under the resources directory.
 * </p>
 */
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

    /**
     * Registers all slash commands defined in commands.yaml for the given guild
     * so that users can see the commands when they type a "/".
     * <p>
     *     It does this by attempting to add an entry to {@link SlashCommands#commandsIndex}
     *     whose key is the command name, and whose value is a new instance of
     *     the handler class which the command has specified.
     * </p>
     * @param guild The guild to update commands for.
     */
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
        event.deferEdit().queue();

        JsonObject root;
        Document document;

        Guild guild = preferredGuild;

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> reactionroles = database.getCollection("reactionroles");
        MongoCollection<Document> openSubmissions = database.getCollection("open_submissions");
        MongoCollection<Document> submissionMessages = database.getCollection("submission_messages");

        switch (id[0]) {
            case "dm-submission":

                document = openSubmissions.find(eq("guild_id", guild.getId())).first();

                root = JsonParser.parseString(document.toJson()).getAsJsonObject();
                String text = root.get("text").getAsString();

                switch (id[1]) {
                    case "send": new SubmissionListener().dmSubmissionSend(event, text); break;
                    case "cancel": new SubmissionListener().dmSubmissionCancel(event); break;
                }
                openSubmissions.deleteOne(document);
                break;

            case "submission":

                document = submissionMessages.find(eq("guild_id", guild.getId())).first();

                root = JsonParser.parseString(document.toJson()).getAsJsonObject();
                String userID = root.get("user_id").getAsString();

                switch (id[1]) {
                    case "approve": new SubmissionListener().submissionApprove(event, userID); break;
                    case "decline": new SubmissionListener().submissionDecline(event); break;
                }
                submissionMessages.deleteOne(document);
                break;

            case "reactionroles":

                String messageID = id[1];
                String buttonLabel = id[2];

                Member member = event.getGuild().retrieveMemberById(event.getUser().getId()).complete();

                BasicDBObject criteria = new BasicDBObject()
                        .append("guild_id", event.getGuild().getId())
                        .append("message_id", messageID)
                        .append("button_label", buttonLabel);

                String JSON = reactionroles.find(criteria).first().toJson();

                JsonObject Root = JsonParser.parseString(JSON).getAsJsonObject();
                String roleID = Root.get("role_id").getAsString();

                Role role = event.getGuild().getRoleById(roleID);

                if (member.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(member, role).queue();
                    event.getHook().sendMessage("Removed Role: " + role.getAsMention()).setEphemeral(true).queue();
                } else {
                    event.getGuild().addRoleToMember(member, role).queue();
                    event.getHook().sendMessage("Added Role: " + role.getAsMention()).setEphemeral(true).queue();
                }
                break;
        }
    }
}
