package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.Constants;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.StringUtils;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.MarkdownUtil;

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
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildBotInfoEmbed(event.getJDA()))
				.addComponents(ActionRow.of(Button.link(Constants.GITHUB_LINK, "View on GitHub")))
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
