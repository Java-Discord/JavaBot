package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetJoinMessage implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String message = event.getOption("message").getAsString();
        new Database().queryConfig(event.getGuild().getId(), "welcome_system.join_msg", message);
        return event.replyEmbeds(Embeds.configEmbed(event, "Welcome Message", "Welcome Message successfully changed to", null, message, true));
    }
}
