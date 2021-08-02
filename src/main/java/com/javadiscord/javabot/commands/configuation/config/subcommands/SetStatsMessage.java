package com.javadiscord.javabot.commands.configuation.config.subcommands;

import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.other.Database;
import com.javadiscord.javabot.other.Embeds;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

public class SetStatsMessage implements SlashCommandHandler {

    @Override
    public ReplyAction handle(SlashCommandEvent event) {
        String message = event.getOption("message").getAsString();
        new Database().queryConfig(event.getGuild().getId(), "other.stats_category.stats_text", message);
        return event.replyEmbeds(Embeds.configEmbed(event, "Stats-Category Message", "Stats-Category Message successfully changed to", null, message, true));
    }
}
