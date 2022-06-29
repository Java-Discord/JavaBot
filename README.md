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
_Work in Progress_
