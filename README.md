# JavaBot

General utility bot for the [JavaDiscord Community](https://join.javadiscord.net/)

# Usage 

To start up, run the bot once, and it will generate a `config` directory. Stop the bot, and set the up **all of the following values**:
- in `systems.json`
  - `jdaBotToken` to your bot's token
- in `{guildId}.json`
  - `moderation.logChannelId` to a channelId
  - `moderation.staffRoleId` to a roleId
  - `moderation.adminRoleId` to a roleId
  - `jam.adminRoleId` to a roleId

Note that this is just what is required for the bot to start. Certain features may require other values to be set.


# Commands
Commands are defined in this bot using a `.yaml` configuration file located in `src/main/resources/commands`. The data in this file is transformed at startup time into an array of `net.javadiscord.javabot.command.data.slash_commands.SlashCommandConfig` objects using JSON deserialization.

These commands are then used by `net.javadiscord.javabot.command.InteractionHandler#registerSlashCommands(Guild)` to register the defined commands as Discord slash commands which become available to users in guilds and private messages with the bot.

**Each command MUST define a `handler` property, whose name is the fully-qualified class name of a `SlashCommand`.** When registering commands, the bot will look for such a class, and attempt to create a new instance of it using a no-args constructor. Therefore, make sure that your handler class has a no-args constructor.

### Privileges
To specify that a command should only be allowed to be executed by certain people, you can specify a list of privileges. For example:
```yaml
- name: jam-admin
  description: Administrator actions for configuring the Java Jam.
  handler: net.javadiscord.javabot.systems.jam.JamAdminCommandHandler
  enabledByDefault: false
  privileges:
    - type: ROLE
      id: jam.adminRoleId
    - type: USER
      id: 235439851263098880
```
In this example, we define that the `jam-admin` command is first of all, *not enabled by default*, and also we say that anyone from the `jam.adminRoleId` role (as found using `Bot.config.getJam().getAdminRoleId()`). Additionally, we also say that the user whose id is `235439851263098880` is allowed to use this command. See `BotConfig#resolve(String)` for more information about how role names are resolved at runtime.

*Context-Commands work in almost the same way, follow the steps above but replace `SlashCommand` with `MessageContextCommand` or `UserContextCommand`.*

### Autocomplete
To enable Autocomplete for a certain Slash Command Option, head to the command's entry in the corresponding 
YAML-File and set the `autocomplete` value.

```yaml
- name: remove
  description: Removes a question from the queue.
  options:
    - name: id
      description: The id of the question to remove.
      required: true
      autocomplete: true
      type: INTEGER
```

Now, you just need to implement `Autocompletable` in your handler class.

```java
@Override
public AutoCompleteCallbackAction handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
    return switch (event.getSubcommandName()) {
        case "remove" -> ListQuestionsSubcommand.replyQuestions(event);
        default -> event.replyChoices();
    };
}
```

The `handleAutocomplete` method of your handler class now gets fired once someone is focusing any Slash Command Option that has the `autocomplete`
property set to true. 


# Configuration
The bot's configuration consists of a collection of simple JSON files:
- `systems.json` contains global settings for the bot's core systems.
- For every guild, a `{guildId}.json` file exists, which contains any guild-specific configuration settings.

At startup, the bot will initially start by loading just the global settings, and then when the Discord ready event is received, the bot will add configuration for each guild it's in, loading it from the matching JSON file, or creating a new file if needed.
