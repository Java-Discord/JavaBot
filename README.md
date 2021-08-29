# JavaBot

General utility bot for the [JavaDiscord Community](https://join.javadiscord.net/)

# Usage 

To start up, run the bot once, and it will generate a `config.json` file. Stop the bot, and set the `jdaBotToken` (under `systems`) to contain your token. Additionally, you should set the `mongoDatabaseUrl` to a URL to your own instance of MongoDB.

# Commands
Commands are defined in this bot using a `commands.yaml` configuration file. The data in this file is transformed at startup time into an array of `com.javadiscord.javabot.properties.command.CommandConfig` objects using JSON deserialization.

These commands are then used by `com.javadiscord.javabot.SlashCommands#registerSlashCommands(Guild)` to register the defined commands as Discord slash commands which become available to users in guilds and private messages with the bot.

**Each command MUST define a `handler` property, whose name is the fully-qualified class name of a `SlashCommandHandler`.** When registering commands, the bot will look for such a class, and attempt to create a new instance of it using a no-args constructor. Therefore, make sure that your handler class has a no-args constructor.

## Privileges
To specify that a command should only be allowed to be executed by certain people, you can specify a list of privileges. For example:
```yaml
- name: jam-admin
  description: Administrator actions for configuring the Java Jam.
  handler: com.javadiscord.javabot.jam.JamAdminCommandHandler
  enabledByDefault: false
  privileges:
    - type: ROLE
      id: jam.adminRoleId
    - type: USER
      id: 235439851263098880
```
In this example, we define that the `jam-admin` command is first of all, *not enabled by default*, and also we say that anyone from the `jam.adminRoleId` role (as found using `Bot.config.getJam().getAdminRoleId()`). Additionally, we also say that the user whose id is `235439851263098880` is allowed to use this command. See `BotConfig#resolve(String)` for more information about how role names are resolved at runtime.
