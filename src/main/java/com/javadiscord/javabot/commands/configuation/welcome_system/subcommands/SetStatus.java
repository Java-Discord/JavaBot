package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetStatus implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        boolean status = event.getOption("status").getAsBoolean();
        //new Database().queryConfig(event.getGuild().getId(), "welcome_system.welcome_status", status);
        return event.replyEmbeds(new Config().configEmbed(
                "Welcome System Status",
                "`" + status + "`"
        ));
    }
}
