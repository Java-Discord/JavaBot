package com.javadiscord.javabot.commands.configuation;

import com.javadiscord.javabot.commands.DelegatingCommandHandler;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class Config extends DelegatingCommandHandler {
    public Config() {
        addSubcommand("list", new ListSubcommand());
        addSubcommand("get", new GetSubcommand());
        addSubcommand("set", new SetSubcommand());
    }
}

