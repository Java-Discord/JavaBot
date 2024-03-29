package net.discordjug.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.discordjug.javabot.data.config.GuildConfigItem;

/**
 * Configuration for the guild's serverlock system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServerLockConfig extends GuildConfigItem {
	private String locked = "false";
	private int minimumAccountAgeInDays = 7;
	private int lockThreshold = 5;
	private float minimumSecondsBetweenJoins = 1.0f;

	public boolean isLocked() {
		return Boolean.parseBoolean(this.locked);
	}
}
