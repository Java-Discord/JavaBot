# JavaBot — General Utility Bot for the [JavaDiscord Community](https://join.javadiscord.net/)

![Banner](https://user-images.githubusercontent.com/48297101/174893242-c8fc553a-e36b-4c5f-91d3-9c3bc659a7c9.png)

# Usage 

To start up, run the bot once, and it will generate a `config` directory. Stop the bot, and set the up **all of the following values**:
- in `systems.json`
  - `jdaBotToken` to your bot's token
  - (some `adminUsers` which, e.g., can manipulate the database)
- in `{guildId}.json`
  - `moderation.logChannelId` to a channelId
  - `moderation.staffRoleId` to a roleId
  - `moderation.adminRoleId` to a roleId
  - `jam.adminRoleId` to a roleId

Note that this is just what is required for the bot to start. Certain features may require other values to be set.

# Configuration

The bot's configuration consists of a collection of simple JSON files:
- `systems.json` contains global settings for the bot's core systems.
- For every guild, a `{guildId}.json` file exists, which contains any guild-specific configuration settings.

At startup, the bot will initially start by loading just the global settings, and then when the Discord ready event is received, the bot will add configuration for each guild it's in, loading it from the matching JSON file, or creating a new file if needed.

# Commands

We're using [DIH4JDA](https://github.com/DynxstyGIT/DIH4JDA) as our Command/Interaction framework, which makes it quite easy to add new commands.

[PingCommand.java](https://github.com/Java-Discord/JavaBot/blob/main/src/main/java/net/javadiscord/javabot/systems/commands/PingCommand.java)
```java
/**
 * <h3>This class represents the /ping command.</h3>
 */
public class PingCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public PingCommand() {
		setCommandData(Commands.slash("ping", "Shows the bot's gateway ping.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getAvatarUrl())
				.setColor(Responses.Type.DEFAULT.getColor())
				.build()
		).queue();
	}
}
```

For more information on how this works, visit the [DIH4JDA Wiki!](https://github.com/DynxstyGIT/DIH4JDA/wiki)

# API Documentation

#### `GET` `guilds/{guild_id}/metrics` 
- Responds with guild-specific metrics, such as the member- and (approximate) online count.

#### `GET` `guilds/{guild_id}/users/{user_id}`
- Responds with the specified users' profile which includes some basic info, such as the users' name and avatar url, but also their recent warns, their current help experience and more!

#### `GET` `guilds/{guild_id}/leaderboard/qotw?page=1`
- A paginated endpoint which responds with an ordered list of users, based on their QOTW points.

#### `GET` `guilds/{guild_id}/leaderboard/experience?page=1`
- A paginated endpoint which responds with an ordered list of users, based on their help channel experience.

You can try out the API yourself on `api.javadiscord.net`! 

# Credits

Inspiration we took from other communities:

- We designed our [Help Channel System](https://github.com/Java-Discord/JavaBot/tree/main/src/main/java/net/javadiscord/javabot/systems/help) similar to the one on the [Python Discord](https://discord.gg/python).
- [`/move-conversation`](https://github.com/Java-Discord/JavaBot/blob/main/src/main/java/net/javadiscord/javabot/systems/user_commands/MoveConversationCommand.java) is heavily inspired by the [Rust Programming Language Community Server](https://discord.gg/rust-lang-community)
