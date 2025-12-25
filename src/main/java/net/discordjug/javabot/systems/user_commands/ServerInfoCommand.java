package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.Constants;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

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
		setCommandData(Commands.slash("serverinfo", "Shows some information about the current server.")
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (event.getGuild() == null) {
			Responses.replyGuildOnly(event).queue();
			return;
		}
		event.deferReply().queue();
		event.getGuild().retrieveOwner().queue(owner -> 
			event
				.getHook()
				.sendMessageEmbeds(
					buildServerInfoEmbed(event.getGuild(), owner))
						.addComponents(ActionRow.of(Button.link(Constants.WEBSITE_LINK, "Website"))
				).queue()
		);
	}

	private @NotNull MessageEmbed buildServerInfoEmbed(@NotNull Guild guild, Member owner) {
		long categories = guild.getCategories().size();
		long channels = guild.getChannels().size() - categories;
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setThumbnail(guild.getIconUrl())
				.setAuthor(guild.getName(), null, guild.getIconUrl())
				.setTitle("Server Information")
				.addField("Owner", owner.getAsMention(), true)
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
