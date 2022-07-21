package net.javadiscord.javabot.systems.custom_commands.model;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

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

	/**
	 * Converts this {@link CustomCommand} into its {@link SlashCommandData}.
	 *
	 * @return The built {@link SlashCommandData}.
	 */
	public SlashCommandData toSlashCommandData() {
		return Commands.slash(name, response.length() > CommandData.MAX_DESCRIPTION_LENGTH ? response.substring(0, CommandData.MAX_DESCRIPTION_LENGTH - 3) + "..." : response)
				.addOption(OptionType.BOOLEAN, "reply", "Should the command reply to you?", false)
				.addOption(OptionType.BOOLEAN, "embed", "Should the response be embedded?", false)
				.setGuildOnly(true);
	}

	/**
	 * Converts this {@link CustomCommand}'s response into a {@link MessageEmbed}.
	 *
	 * @return The built {@link MessageEmbed}.
	 */
	public MessageEmbed toEmbed() {
		return new EmbedBuilder().setDescription(response).build();
	}
}
