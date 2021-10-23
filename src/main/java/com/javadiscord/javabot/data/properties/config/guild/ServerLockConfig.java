package com.javadiscord.javabot.data.properties.config.guild;

import com.javadiscord.javabot.data.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerLockConfig extends GuildConfigItem {
    private boolean locked = false;
    private int minimumAccountAgeInDays = 7;
    private int lockThreshold = 5;
}
