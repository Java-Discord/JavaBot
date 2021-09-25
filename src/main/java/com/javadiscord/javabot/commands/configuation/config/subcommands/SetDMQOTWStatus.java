package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetDMQOTWStatus implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        boolean status = event.getOption("enabled").getAsBoolean();
        //new Database().queryConfig(event.getGuild().getId(), "other.qotw.dm-qotw", status);
        return event.replyEmbeds(new Config().configEmbed(
                "DM-QOTW Status",
                "`" + status + "`"
        ));
    }
}
