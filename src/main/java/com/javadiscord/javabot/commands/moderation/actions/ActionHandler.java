package com.javadiscord.javabot.commands.moderation.actions;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

public interface ActionHandler {
    void handle (Object ev, Member member, User author, String reason);
}
