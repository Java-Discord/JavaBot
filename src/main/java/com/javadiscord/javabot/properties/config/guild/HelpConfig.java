package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;

@Data
@EqualsAndHashCode(callSuper = true)
public class HelpConfig extends GuildConfigItem {
    private long helpRoleId;

    public Role getHelpRole() {
        return this.getGuild().getRoleById(this.helpRoleId);
    }
}
