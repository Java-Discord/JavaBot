package net.javadiscord.javabot.systems.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.StringUtils;

import java.time.Instant;

/**
 * Command that provides some basic info about the bot.
 */
@Slf4j
public class BotInfoCommand extends SlashCommand {
	public BotInfoCommand() {
		setCommandData(Commands.slash("botinfo", "Shows some information about the Bot"));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildBotInfoEmbed(event.getJDA(), Bot.config.get(event.getGuild()).getSlashCommand()))
				.addActionRow(Button.link(Constants.GITHUB_LINK, "View on GitHub"))
				.queue();
	}

	private MessageEmbed buildBotInfoEmbed(JDA jda, SlashCommandConfig config) {
		SelfUser bot = jda.getSelfUser();
		return new EmbedBuilder()
				.setColor(config.getDefaultColor())
				.setThumbnail(bot.getEffectiveAvatarUrl())
				.setAuthor(bot.getAsTag(), null, bot.getEffectiveAvatarUrl())
				.setTitle("Info")
				.addField("OS", String.format("```%s```", StringUtils.getOperatingSystem()), true)
				.addField("Library", "```JDA```", true)
				.addField("JDK", String.format("```%s```", System.getProperty("java.version")), true)
				.addField("Gateway Ping", String.format("```%sms```", jda.getGatewayPing()), true)
				.addField("Uptime", String.format("```%s```", new UptimeCommand().getUptime()), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
