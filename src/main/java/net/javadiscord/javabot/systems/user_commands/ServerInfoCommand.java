package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * <h3>This class represents the /serverinfo command.</h3>
 */
public class ServerInfoCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public ServerInfoCommand() {
		setSlashCommandData(Commands.slash("serverinfo", "Shows some information about the current server.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		event.replyEmbeds(buildServerInfoEmbed(event.getGuild()))
				.addActionRow(Button.link(Constants.WEBSITE_LINK, "Website")).queue();
	}

	private @NotNull MessageEmbed buildServerInfoEmbed(@NotNull Guild guild) {
		long categories = guild.getCategories().size();
		long channels = guild.getChannels().size() - categories;
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setThumbnail(guild.getIconUrl())
				.setAuthor(guild.getName(), null, guild.getIconUrl())
				.setTitle("Server Information")
				.addField("Owner", guild.getOwner().getAsMention(), true)
				.addField("Member Count", guild.getMemberCount() + " members", true)
				.addField("Roles", String.format("%s Roles", guild.getRoles().size() - 1L), true)
				.addField("ID", String.format("```%s```", guild.getIdLong()), false)
				.addField("Channel Count",
						String.format("""
								```%s Channels, %s Categories
								Text: %s
								Voice: %s```""", channels, categories, guild.getTextChannels().size(), guild.getVoiceChannels().size()), false)
				.addField("Server created on", String.format("<t:%s:f>", guild.getTimeCreated().toInstant().getEpochSecond()), false)
				.setTimestamp(Instant.now())
				.build();
	}
}
