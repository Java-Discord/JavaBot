package net.javadiscord.javabot.systems.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;

/**
 * Command that provides some basic info about the bot.
 */
@Slf4j
public class BotInfoCommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var embed = buildBotInfoEmbed(event.getJDA(), Bot.config.get(event.getGuild()).getSlashCommand());
		return event.replyEmbeds(embed).addActionRow(Button.link(Constants.GITHUB_LINK, "View on GitHub")
		);
	}

	private MessageEmbed buildBotInfoEmbed(JDA jda, SlashCommandConfig config) {
		var bot = jda.getSelfUser();
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
