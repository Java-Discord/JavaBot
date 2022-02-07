package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Emote;
import net.javadiscord.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guild's emotes.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmoteConfig extends GuildConfigItem {
	private String failureId;
	private String successId;
	private String upvoteId;
	private String downvoteId;
	private String trashBin;

	public Emote getFailureEmote() {
		return getGuild().getJDA().getEmoteById(this.failureId);
	}

	public Emote getSuccessEmote() {
		return getGuild().getJDA().getEmoteById(this.successId);
	}

	public Emote getUpvoteEmote() {
		return getGuild().getJDA().getEmoteById(this.upvoteId);
	}

	public Emote getDownvoteEmote() {
		return getGuild().getJDA().getEmoteById(this.downvoteId);
	}
}

