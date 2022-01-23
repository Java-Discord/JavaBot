package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.NewsChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.javadiscord.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guild's qotw system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QOTWConfig extends GuildConfigItem {
	private long questionChannelId;
	private long submissionChannelId;
	private long questionRoleId;
	private long qotwReviewRoleId;

	public NewsChannel getQuestionChannel() {
		return this.getGuild().getNewsChannelById(this.questionChannelId);
	}

	public TextChannel getSubmissionChannel() {
		return this.getGuild().getTextChannelById(this.submissionChannelId);
	}

	public Role getQOTWRole() {
		return this.getGuild().getRoleById(this.questionRoleId);
	}

	public Role getQOTWReviewRole() {
		return this.getGuild().getRoleById(this.qotwReviewRoleId);
	}
}
