package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetStatsMessage implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String message = event.getOption("message").getAsString();
        Database.queryConfig(event.getGuild().getId(), "other.stats_category.stats_text", message);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category Message", "Stats-Category Message succesfully changed to", null, message, true)).queue();

    }
}
