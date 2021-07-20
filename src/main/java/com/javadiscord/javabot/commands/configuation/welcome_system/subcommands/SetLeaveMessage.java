package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetLeaveMessage implements WelcomeCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String message = event.getOption("message").getAsString();
        new Database().queryConfig(event.getGuild().getId(), "welcome_system.leave_msg", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Leave Message", "Leave Message successfully changed to", null, event.getOption("message").getAsString(), true)).queue();
    }
}
