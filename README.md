# JavaBot

General utility bot for the [JavaDiscord Community](https://join.javadiscord.net/)

# Usage 

To start up, the bot only needs a ``bot.props`` file in the source directory. 
The file must contain a Discord Bot Token and a MongoDB Connection String.

```
token=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
mongologin=mongodb+srv\://<USER>\:<PASSWORD>@<CLUSTERURL>
```

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
      id: jam_admin_rid
    - type: USER
      id: 235439851263098880
```
In this example, we define that the `jam-admin` command is first of all, *not enabled by default*, and also we say that anyone from the `jam_admin_rid` role (as found using `new Database().getConfigRole(guild, "roles." + this.id);` from the MongoDB). Additionally, we also say that the user whose id is `235439851263098880` is allowed to use this command.
