package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetJoinMessage implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String message = event.getOption("message").getAsString();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.join_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Message", "Welcome Message successfully changed to", null, message, true)).queue();
    }
}
