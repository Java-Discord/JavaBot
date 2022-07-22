package net.javadiscord.javabot.systems.qotw.commands.qotw_points;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.systems.notification.GuildNotificationService;
import net.javadiscord.javabot.systems.notification.QOTWNotificationService;
import net.javadiscord.javabot.systems.qotw.QOTWPointsService;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * <h3>This class represents the /qotw account increment command.</h3>
 * This Subcommand allows staff-members to increment the QOTW-Account of any user.
 */
public class IncrementPointsSubcommand extends SlashCommand.Subcommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public IncrementPointsSubcommand() {
		setSubcommandData(new SubcommandData("increment", "Adds one point to the user's QOTW-Account")
				.addOption(OptionType.USER, "user", "The user whose points should be incremented.", true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		var userOption = event.getOption("user");
		if (userOption == null) {
			Responses.missingArguments(event).queue();
			return;
		}
		Member member = userOption.getAsMember();
		if (member == null) {
			Responses.error(event, "User must be a part of this server.").queue();
			return;
		}
		event.deferReply().queue();
		QOTWPointsService service = new QOTWPointsService(Bot.dataSource);
		long points = service.increment(member.getIdLong());
		MessageEmbed embed = buildIncrementEmbed(member.getUser(), points);
		new GuildNotificationService(event.getGuild()).sendLogChannelNotification(embed);
		new QOTWNotificationService(member.getUser(), event.getGuild()).sendAccountIncrementedNotification();
		event.getHook().sendMessageEmbeds(embed).queue();
	}

	private @NotNull MessageEmbed buildIncrementEmbed(@NotNull User user, long points) {
		QOTWPointsService service = new QOTWPointsService(Bot.dataSource);
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("QOTW Account Incremented")
				.setColor(Responses.Type.SUCCESS.getColor())
				.addField("Total QOTW-Points", "```" + points + "```", true)
				.addField("Rank", "```#" + service.getQOTWRank(user.getIdLong()) + "```", true)
				.setFooter("ID: " + user.getId())
				.setTimestamp(Instant.now())
				.build();
	}
}
