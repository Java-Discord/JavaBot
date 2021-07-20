package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.configuation.config.ConfigCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public class SetStatsCategory implements ConfigCommandHandler {

    @Override
    public void handle(SlashCommandEvent event) {

        String id = event.getOption("id").getAsString();
        new Database().queryConfig(event.getGuild().getId(), "other.stats_category.stats_cid", id);
        event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category ID", "Stats-Category ID succesfully changed to", null, id, true)).queue();

    }
}
