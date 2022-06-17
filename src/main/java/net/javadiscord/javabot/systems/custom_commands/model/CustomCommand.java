package net.javadiscord.javabot.systems.custom_commands.model;

import lombok.Data;

/**
 * A data class that represents a single Custom Command.
 */
@Data
public class CustomCommand {
	private long id;
	private long guildId;
	private long createdBy;
	private String name;
	private String response;
	private boolean reply;
	private boolean embed;
}
