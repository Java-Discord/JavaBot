package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetStatus implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        boolean status = event.getOption("status").getAsBoolean();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.welcome_status", status);
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome System Status changed", "Status successfully changed to ", null, String.valueOf(status), true)).queue();
    }
}
