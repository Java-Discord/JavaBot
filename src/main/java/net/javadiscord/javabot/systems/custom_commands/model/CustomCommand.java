package net.javadiscord.javabot.systems.custom_commands.model;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.time.Instant;

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

	public SlashCommandData toSlashCommandData() {
		return Commands.slash(name, response.substring(0, Math.min(response.length(), 100)))
				.setGuildOnly(true);
	}

	public MessageEmbed toEmbed() {
		return new EmbedBuilder()
				.setDescription(response)
				.setTimestamp(Instant.now())
				.build();
	}
}
