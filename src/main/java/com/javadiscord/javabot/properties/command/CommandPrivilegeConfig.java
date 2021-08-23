package com.javadiscord.javabot.properties.command;

import com.javadiscord.javabot.other.Database;
import lombok.Data;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.util.concurrent.CompletableFuture;

public record CommandPrivilegeConfig(String type, boolean enabled, String id) {
	public CompletableFuture<CommandPrivilege> toData(Guild guild, Database database) {
		if (this.type.equalsIgnoreCase(CommandPrivilege.Type.USER.name())) {
			return guild.getJDA().retrieveUserById(this.id).submit()
					.thenCompose(user -> {
						if (user == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid user id."));
						return CompletableFuture.completedFuture(new CommandPrivilege(CommandPrivilege.Type.USER, this.enabled, user.getIdLong()));
					});
		} else if (this.type.equalsIgnoreCase(CommandPrivilege.Type.ROLE.name())) {
			Role role = database.getConfigRole(guild, "roles." + this.id);
			if (role == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid role id. Should refer to a roles.{...} config property."));
			return CompletableFuture.completedFuture(new CommandPrivilege(CommandPrivilege.Type.ROLE, this.enabled, role.getIdLong()));
		}
		return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid type."));
	}
}
