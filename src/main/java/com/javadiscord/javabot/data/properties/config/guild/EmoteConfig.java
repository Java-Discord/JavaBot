package com.javadiscord.javabot.data.properties.config.guild;

import com.javadiscord.javabot.data.properties.config.GuildConfigItem;
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

    public Emote getLoadingEmote() { return getGuild().getJDA().getEmoteById(this.loadingId); }

    public Emote getFailureEmote() { return getGuild().getJDA().getEmoteById(this.failureId); }

    public Emote getSuccessEmote() { return getGuild().getJDA().getEmoteById(this.successId); }

    public Emote getUpvoteEmote() { return getGuild().getJDA().getEmoteById(this.upvoteId); }

    public Emote getDownvoteEmote() { return getGuild().getJDA().getEmoteById(this.downvoteId); }

    public Emote getSpotifyEmote() { return getGuild().getJDA().getEmoteById(this.spotifyId); }

    public Emote getOnlineEmote() { return getGuild().getJDA().getEmoteById(this.onlineId); }

    public Emote getOfflineEmote() { return getGuild().getJDA().getEmoteById(this.offlineId); }

    public Emote getIdleEmote() { return getGuild().getJDA().getEmoteById(this.idleId); }

    public Emote getDndEmote() { return getGuild().getJDA().getEmoteById(this.dndId); }

    public Emote getBalanceEmote() { return getGuild().getJDA().getEmoteById(this.balanceId); }

    public Emote getBraveryEmote() { return getGuild().getJDA().getEmoteById(this.braveryId); }

    public Emote getBrillianceEmote() { return getGuild().getJDA().getEmoteById(this.brillianceId); }

    public Emote getDevEmote() { return getGuild().getJDA().getEmoteById(this.devId); }

    public Emote getBugHunterEmote() { return getGuild().getJDA().getEmoteById(this.bugHunterId); }

    public Emote getStaffEmote() { return getGuild().getJDA().getEmoteById(this.staffId); }

    public Emote getServerBoostEmote() { return getGuild().getJDA().getEmoteById(this.serverBoostId); }

    public Emote getPartnerEmote() { return getGuild().getJDA().getEmoteById(this.partnerId); }

    public Emote getEarlySupporterEmote() { return getGuild().getJDA().getEmoteById(this.earlySupporterId); }

    public Emote getBotEmote() { return getGuild().getJDA().getEmoteById(this.botId); }
}

