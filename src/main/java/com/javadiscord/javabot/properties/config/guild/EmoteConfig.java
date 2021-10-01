package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmoteConfig extends GuildConfigItem {
    private String loadingEmote;
    private String failureEmote;
    private String successEmote;
    private String spotifyEmote;
    private String onlineEmote;
    private String offlineEmote;
    private String idleEmote;

    private String dndBadge;
    private String balanceBadge;
    private String braveryBadge;
    private String brillianceBadge;
    private String devBadge;
    private String bugHunterBadge;
    private String staffBadge;
    private String serverBoostBadge;
    private String partnerBadge;
    private String earlySupporterBadge;
    private String botBadge;

    private String failureReaction;
    private String successReaction;
    private String upvoteReaction;
    private String downvoteReaction;
}
