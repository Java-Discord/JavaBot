package net.javadiscord.javabot.command.data.context_commands;

import lombok.Data;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Simple DTO representing a top-level Discord context command.
 */
@Data
public class ContextCommandConfig {
	private String name;
	private String type;
	private String handler;

	public Command.Type getType() {
		return Command.Type.valueOf(this.type);
	}

	public CommandData toData() {
		return Commands.context(getType(), this.name);
	}
}
