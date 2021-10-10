package com.javadiscord.javabot.properties.config.guild;

import com.javadiscord.javabot.properties.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class RolesConfig extends GuildConfigItem {
    /**
     * The id of the helper guild role.
     */
    private Map<String, Long> roleMap;

    public Role lookupRoleByName(String name, Guild guild) {
        if(roleMap.containsKey(name)){
            return guild.getRoleById(roleMap.get(name));
        }
        throw new IllegalArgumentException(String.format("Role \"%s\" was not found", name));
    }
}
