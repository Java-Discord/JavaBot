package com.javadiscord.javabot.service.serverlock.subcommands;

import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.data.mongodb.Database;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

/**
 * Command that allows users to change the status of the server lock
 */
public class SetServerLockStatus implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        var status = event.getOption("status").getAsBoolean();
            new Database().setConfigEntry(event.getGuild().getId(), "other.server_lock.lock_status", status);
            return Responses.info(event, "Server Lock", String.format("Successfully set the lock-status to `%s`", status));
    }
}
