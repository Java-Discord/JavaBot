package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetLockStatus implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        boolean status = event.getOption("locked").getAsBoolean();
        Database.queryConfig(event.getGuild().getId(), "other.server_lock.lock_status", status);
        event.replyEmbeds(Embeds.configEmbed(event, "Lock Status changed", "Lock Status succesfully changed to ", null, Boolean.toString(status), true)).queue();

    }
}
