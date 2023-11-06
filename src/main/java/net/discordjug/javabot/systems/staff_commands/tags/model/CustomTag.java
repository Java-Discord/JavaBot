package net.discordjug.javabot.systems.staff_commands.tags.model;

import lombok.Data;
import net.discordjug.javabot.systems.staff_commands.tags.CustomTagManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import org.jetbrains.annotations.NotNull;

/**
 * A data class that represents a single Custom Command.
 */
@Data
public class CustomTag {
	private long id;
	private long guildId;
	private long createdBy;
	private String name;
	private String response;
	private boolean reply;
	private boolean embed;

	public void setName(@NotNull String name) {
		this.name = CustomTagManager.cleanString(name);
	}

	/**
	 * Converts this {@link CustomTag}'s response into a {@link MessageEmbed}.
	 *
	 * @return The built {@link MessageEmbed}.
	 */
	public MessageEmbed toEmbed() {
		return new EmbedBuilder().setDescription(response).build();
	}
}
