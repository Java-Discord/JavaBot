package net.javadiscord.javabot.systems.user_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.util.StringUtils;

import java.time.Instant;

/**
 * Command that provides some basic info about the bot.
 */
public class BotInfoCommand extends SlashCommand {
	public BotInfoCommand() {
		setSlashCommandData(Commands.slash("botinfo", "Shows some information about the Bot")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildBotInfoEmbed(event.getJDA(), Bot.config.get(event.getGuild()).getSlashCommand()))
				.addActionRow(Button.link(Constants.GITHUB_LINK, "View on GitHub"))
				.queue();
	}

	private MessageEmbed buildBotInfoEmbed(JDA jda, SlashCommandConfig config) {
		return new EmbedBuilder()
				.setColor(config.getDefaultColor())
				.setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl())
				.setAuthor(jda.getSelfUser().getAsTag(), null, jda.getSelfUser().getEffectiveAvatarUrl())
				.setTitle("Info")
				.addField("OS", MarkdownUtil.codeblock(StringUtils.getOperatingSystem()), true)
				.addField("Library", MarkdownUtil.codeblock("JDA"), true)
				.addField("JDK", MarkdownUtil.codeblock(System.getProperty("java.version")), true)
				.addField("Gateway Ping", MarkdownUtil.codeblock(jda.getGatewayPing() + "ms"), true)
				.addField("Uptime", MarkdownUtil.codeblock(UptimeCommand.getUptime()), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
