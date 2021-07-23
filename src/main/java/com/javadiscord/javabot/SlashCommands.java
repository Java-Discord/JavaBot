package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.properties.command.CommandConfig;
import com.javadiscord.javabot.properties.command.CommandDataConfig;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
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
}
