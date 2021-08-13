package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetDMQOTWStatus implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        boolean status = event.getOption("enabled").getAsBoolean();
        Database.queryConfig(event.getGuild().getId(), "other.qotw.dm-qotw", status);
        return event.replyEmbeds(Embeds.configEmbed(
                event,
                "QOTW-DM Status",
                "QOTW-DM Status successfully changed to",
                null,
                String.valueOf(status),
                true
        ));
    }
}
