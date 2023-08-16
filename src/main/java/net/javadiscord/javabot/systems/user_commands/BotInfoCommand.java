package net.javadiscord.javabot.systems.user_commands;

import net.javadiscord.javabot.util.UserUtils;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.javadiscord.javabot.util.Constants;
import net.javadiscord.javabot.util.Responses;
import net.javadiscord.javabot.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * This class represents the `/botinfo` command.
 */
public class BotInfoCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public BotInfoCommand() {
		setCommandData(Commands.slash("botinfo", "Shows some information about the Bot.")
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildBotInfoEmbed(event.getJDA()))
				.addActionRow(Button.link(Constants.GITHUB_LINK, "View on GitHub"))
				.queue();
	}

	private @NotNull MessageEmbed buildBotInfoEmbed(@NotNull JDA jda) {
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl())
				.setAuthor(UserUtils.getUserTag(jda.getSelfUser()), null, jda.getSelfUser().getEffectiveAvatarUrl())
				.setTitle("Bot Information")
				.addField("OS", MarkdownUtil.codeblock(StringUtils.getOperatingSystem()), true)
				.addField("Uptime", MarkdownUtil.codeblock(StringUtils.formatUptime()), true)
				.addField("Library", MarkdownUtil.codeblock("JDA"), true)
				.addField("JDK", MarkdownUtil.codeblock(System.getProperty("java.version")), true)
				.addField("Gateway Ping", MarkdownUtil.codeblock(jda.getGatewayPing() + "ms"), true)
				.setTimestamp(Instant.now())
				.build();
	}
}
