package com.javadiscord.javabot.commands.configuation.welcome_system.subcommands;

import com.javadiscord.javabot.commands.configuation.welcome_system.WelcomeCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetWelcomeChannel implements WelcomeCommandHandler {
    @Override
    public void handle(SlashCommandEvent event) {

        MessageChannel channel = event.getOption("channel").getAsMessageChannel();
        Database.queryConfig(event.getGuild().getId(), "welcome_system.welcome_cid", channel.getId());
        event.replyEmbeds(Embeds.configEmbed(event, "Welcome Channel", "Welcome Channel successfully changed to", null, channel.getId(), true, true)).queue();
    }
}
