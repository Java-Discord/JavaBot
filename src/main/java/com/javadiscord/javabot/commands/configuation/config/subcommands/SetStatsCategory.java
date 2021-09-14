package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetStatsCategory implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String id = event.getOption("id").getAsString();
        //new Database().queryConfig(event.getGuild().getId(), "other.stats_category.stats_cid", id);
        return event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category ID", "Stats-Category ID successfully changed to", null, id, true));
    }
}
