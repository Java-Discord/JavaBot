package net.javadiscord.javabot.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.javadiscord.javabot.data.config.GuildConfigItem;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerLockConfig extends GuildConfigItem {
    private String locked = "false";
    private int minimumAccountAgeInDays = 7;
    private int lockThreshold = 5;
    private float minimumSecondsBetweenJoins = 1.0f;

    @Deprecated
    private String lockMessageTemplate;

    public boolean isLocked() {
        return Boolean.parseBoolean(this.locked);
    }
}
