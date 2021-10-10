package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.properties.command.CommandConfig;
import com.javadiscord.javabot.properties.command.CommandDataConfig;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;

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
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

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
            command.handle(event).queue();
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
            String value = Root.get("value").getAsString()
                .replace("{!membercount}", String.valueOf(event.getGuild().getMemberCount()))
                .replace("{!servername}", event.getGuild().getName())
                .replace("{!serverid}", event.getGuild().getId());

            event.replyEmbeds(new EmbedBuilder().setColor(Color.decode(
                    Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())).setDescription(value).build()).queue();

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
        CommandConfig[] commandConfigs = CommandDataConfig.load();
        var commandUpdateAction = this.updateCommands(commandConfigs, guild);
        this.updateCustomCommands(commandUpdateAction, guild);

        // Add privileges to the commands, after the commands have been registered.
        commandUpdateAction.queue(commands ->
                this.addCommandPrivileges(commands, commandConfigs, guild));
        }


    private CommandListUpdateAction updateCommands(CommandConfig[] commandConfigs, Guild guild) {
        log.info("{}[{}]{} Registering slash commands",
                Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
        if (commandConfigs.length > 100) throw new IllegalArgumentException("Cannot add more than 100 commands.");
        CommandListUpdateAction commandUpdateAction = guild.updateCommands();
        for (CommandConfig config : commandConfigs) {
            if (config.getHandler() != null && !config.getHandler().isEmpty()) {
                try {
                    Class<?> handlerClass = Class.forName(config.getHandler());
                    this.commandsIndex.put(config.getName(), (SlashCommandHandler) handlerClass.getConstructor().newInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                log.warn("Command \"{}\" does not have an associated handler class. It will be ignored.", config.getName());
            }
            commandUpdateAction.addCommands(config.toData());
        }
        return commandUpdateAction;
    }

    private void updateCustomCommands(CommandListUpdateAction commandUpdateAction, Guild guild) {
        log.info("{}[{}]{} Registering custom commands",
                Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);
        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");

        for (Document document : collection.find(eq("guild_id", guild.getId()))) {
            JsonObject Root = JsonParser.parseString(document.toJson()).getAsJsonObject();
            String commandName = Root.get("commandname").getAsString();
            String value = Root.get("value").getAsString();
            if (value.length() > 100) value = value.substring(0, 97) + "...";
            commandUpdateAction.addCommands(new CommandData(commandName, value));
        }
    }

    private void addCommandPrivileges(List<Command> commands, CommandConfig[] commandConfigs, Guild guild) {
        log.info("{}[{}]{} Adding command privileges",
                Constants.TEXT_WHITE, guild.getName(), Constants.TEXT_RESET);

        Map<String, Collection<? extends CommandPrivilege>> map = new HashMap<>();

        for(Command command: commands) {
            List<CommandPrivilege> privileges = getCommandPrivileges(guild, findCommandConfig(command.getName(), commandConfigs));
            if(!privileges.isEmpty()) {
                map.put(command.getId(), privileges);
            }
        }

        guild.updateCommandPrivileges(map)
                .queue(success -> log.info("Commands updated succuessfully"), error -> log.info("Commands update failed"));
    }

    @NotNull
    private List<CommandPrivilege> getCommandPrivileges(Guild guild, CommandConfig config) {
        if(config == null || config.getPrivileges() == null) return Collections.emptyList();
        List<CommandPrivilege> privileges = new ArrayList<>();
        for (var privilegeConfig : config.getPrivileges()) {
            try {
                privileges.add(privilegeConfig.toData(guild, Bot.config));
                log.info("\t{}[{}]{} Registering privilege: {}",
                        Constants.TEXT_WHITE, config.getName(), Constants.TEXT_RESET, Objects.toString(privilegeConfig));
            } catch (Exception e) {
                log.warn("Could not register privileges for command {}: {}", config.getName(), e.getMessage());
            }
        }
        return privileges;
    }

    private CommandConfig findCommandConfig(String name, CommandConfig[] configs) {
        for (CommandConfig config : configs) {
            if (name.equals(config.getName())) {
                return config;
            }
        }
        log.warn("Could not find CommandConfig for command :{}", name);
        return null;
    }
}
