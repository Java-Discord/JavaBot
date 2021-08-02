package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetLockStatus implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        boolean status = event.getOption("locked").getAsBoolean();
        new Database().queryConfig(event.getGuild().getId(), "other.server_lock.lock_status", status);
        return event.replyEmbeds(Embeds.configEmbed(event, "Lock Status changed", "Lock Status successfully changed to ", null, Boolean.toString(status), true));
    }
}
