package com.javadiscord.javabot.properties.command;

import com.javadiscord.javabot.properties.config.BotConfig;
import com.javadiscord.javabot.properties.config.UnknownPropertyException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
public class CommandPrivilegeConfig {
	private String type;
	private boolean enabled = true;
	private String id;

	public CompletableFuture<CommandPrivilege> toData(Guild guild, BotConfig botConfig) {
		if (this.type.equalsIgnoreCase(CommandPrivilege.Type.USER.name())) {
			return guild.getJDA().retrieveUserById(this.id).submit()
					.thenCompose(user -> {
						if (user == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid user id."));
						return CompletableFuture.completedFuture(new CommandPrivilege(CommandPrivilege.Type.USER, this.enabled, user.getIdLong()));
					});
		} else if (this.type.equalsIgnoreCase(CommandPrivilege.Type.ROLE.name())) {
			Long roleId = null;
			try {
				roleId = (Long) botConfig.get(guild).resolve(this.id);
			} catch (UnknownPropertyException e) {
				log.error("Unknown property while resolving role id.", e);
			}
			if (roleId == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Missing role id."));
			Role role = guild.getRoleById(roleId);
			if (role == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Role could not be found for id " + roleId));
			return CompletableFuture.completedFuture(new CommandPrivilege(CommandPrivilege.Type.ROLE, this.enabled, role.getIdLong()));
		}
		return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid type."));
	}
}
