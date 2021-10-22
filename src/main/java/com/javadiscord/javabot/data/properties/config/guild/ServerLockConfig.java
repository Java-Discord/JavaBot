package com.javadiscord.javabot.data.properties.config.guild;

import com.javadiscord.javabot.data.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.TextChannel;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerLockConfig extends GuildConfigItem {
    private int accountAgeThreshold = 7;
    private int lockThreshold = 5;
}
