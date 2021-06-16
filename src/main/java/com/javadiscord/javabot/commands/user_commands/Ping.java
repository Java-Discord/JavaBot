package com.javadiscord.javabot.commands.user_commands;

import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.SlashEnabledCommand;
import com.javadiscord.javabot.other.SlashEnabledCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class Ping extends SlashEnabledCommand {
    public Ping() {
        this.name = "ping";
        this.aliases = new String[]{"pong"};
        this.category = new Category("USER COMMANDS");
        this.help = "Checks Java's gateway ping";
    }

    @Override
    protected void execute(SlashEnabledCommandEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        String botImage = event.getJDA().getSelfUser().getAvatarUrl();
        var e = new EmbedBuilder()
            .setAuthor(gatewayPing + "ms", null, botImage)
            .setColor(Constants.GRAY)
            .build();
        event.reply(e);
    }
}