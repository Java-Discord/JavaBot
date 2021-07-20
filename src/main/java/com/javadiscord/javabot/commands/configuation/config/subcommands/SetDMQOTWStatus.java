package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetDMQOTWStatus implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        boolean status = event.getOption("enabled").getAsBoolean();
        new Database().queryConfig(event.getGuild().getId(), "other.qotw.dm-qotw", status);
        event.replyEmbeds(Embeds.configEmbed(event, "QOTW-DM Status", "QOTW-DM Status succesfully changed to", null, String.valueOf(status), true)).queue();

    }
}
