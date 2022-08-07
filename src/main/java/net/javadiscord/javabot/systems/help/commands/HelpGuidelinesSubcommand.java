package net.javadiscord.javabot.systems.help.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringResourceCache;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Shows the server's help-guidelines.
 */
public class HelpGuidelinesSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public HelpGuidelinesSubcommand() {
		setSubcommandData(new SubcommandData("guidelines", "Show the server's help guidelines in a simple format."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		HelpConfig config = Bot.getConfig().get(event.getGuild()).getHelpConfig();
		String channels = "N/A";
		if (config.getOpenChannelCategory() != null) {
			channels = config.getOpenChannelCategory().getChannels()
					.stream()
					.map(GuildChannel::getAsMention)
					.collect(Collectors.joining("\n"));
		}
		event.replyEmbeds(new EmbedBuilder()
				.setTitle("Help Guidelines")
				.setColor(Responses.Type.DEFAULT.getColor())
				.setDescription(StringResourceCache.load("/help_guidelines/guidelines_text.txt"))
				.addField("Available Help Channels", channels, false)
				.setImage(StringResourceCache.load("/help_guidelines/guidelines_image_url.txt"))
				.build()
		).queue();
	}
}
