package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long reportChannelId;
	private long logChannelId;
	private long suggestionChannelId;
	private long muteRoleId;
	private long staffRoleId;
	private int purgeMaxMessageCount = 1000;
	private long helpGuidelinesChannelId;
	private long helpGuidelinesMessageId;
	private String helpRoleImoji = ":white_check_mark";
	private long verifiedHelpRoleId;

	public Role getVerifiedHelpRole(){ return this.getGuild().getRoleById(this.verifiedHelpRoleId); }

	public TextChannel getHelpGuidelinesChannel(){return this.getGuild().getTextChannelById(helpGuidelinesChannelId);}

	public Message getHelpGuidelinesMessage(){ return this.getGuild().getTextChannelById(helpGuidelinesChannelId).getHistory().getMessageById(helpGuidelinesMessageId);}

	public TextChannel getReportChannel() {
		return this.getGuild().getTextChannelById(this.reportChannelId);
	}

	public TextChannel getLogChannel() {
		return this.getGuild().getTextChannelById(this.logChannelId);
	}

	public TextChannel getSuggestionChannel() {
		return this.getGuild().getTextChannelById(this.suggestionChannelId);
	}

	public Role getMuteRole() {
		return this.getGuild().getRoleById(this.muteRoleId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}
}
