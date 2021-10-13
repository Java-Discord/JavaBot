package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Emote;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmoteConfig extends GuildConfigItem {
    private String loadingId;
    private String failureId;
    private String successId;
    private String upvoteId;
    private String downvoteId;
    private String spotifyId;
    private String onlineId;
    private String offlineId;
    private String idleId;
    private String dndId;

    private String balanceId;
    private String braveryId;
    private String brillianceId;
    private String devId;
    private String bugHunterId;
    private String staffId;
    private String serverBoostId;
    private String partnerId;
    private String earlySupporterId;
    private String botId;

    public Emote getLoadingEmote() { return Bot.jda.getEmoteById(this.loadingId); }

    public Emote getFailureEmote() { return Bot.jda.getEmoteById(this.failureId); }

    public Emote getSuccessEmote() { return Bot.jda.getEmoteById(this.successId); }

    public Emote getUpvoteEmote() { return Bot.jda.getEmoteById(this.upvoteId); }

    public Emote getDownvoteEmote() { return Bot.jda.getEmoteById(this.downvoteId); }

    public Emote getSpotifyEmote() { return Bot.jda.getEmoteById(this.spotifyId); }

    public Emote getOnlineEmote() { return Bot.jda.getEmoteById(this.onlineId); }

    public Emote getOfflineEmote() { return Bot.jda.getEmoteById(this.offlineId); }

    public Emote getIdleEmote() { return Bot.jda.getEmoteById(this.idleId); }

    public Emote getDndEmote() { return Bot.jda.getEmoteById(this.dndId); }

    public Emote getBalanceEmote() { return Bot.jda.getEmoteById(this.balanceId); }

    public Emote getBraveryEmote() { return Bot.jda.getEmoteById(this.braveryId); }

    public Emote getBrillianceEmote() { return Bot.jda.getEmoteById(this.brillianceId); }

    public Emote getDevEmote() { return Bot.jda.getEmoteById(this.devId); }

    public Emote getBugHunterEmote() { return Bot.jda.getEmoteById(this.bugHunterId); }

    public Emote getStaffEmote() { return Bot.jda.getEmoteById(this.staffId); }

    public Emote getServerBoostEmote() { return Bot.jda.getEmoteById(this.serverBoostId); }

    public Emote getPartnerEmote() { return Bot.jda.getEmoteById(this.partnerId); }

    public Emote getEarlySupporterEmote() { return Bot.jda.getEmoteById(this.earlySupporterId); }

    public Emote getBotEmote() { return Bot.jda.getEmoteById(this.botId); }
}

