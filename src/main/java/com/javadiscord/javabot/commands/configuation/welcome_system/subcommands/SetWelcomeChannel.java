package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.configuation.config.Config;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetWelcomeChannel implements SlashCommandHandler {
    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        //new Database().queryConfig(event.getGuild().getId(), "welcome_system.welcome_cid", channel.getId());
        return event.replyEmbeds(new Config().configEmbed(
                "Welcome Channel",
                "<#" + channel.getId() + ">"
        ));
    }
}
