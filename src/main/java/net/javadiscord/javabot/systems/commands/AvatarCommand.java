package net.javadiscord.javabot.systems.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.util.Responses;

import javax.annotation.Nullable;

/**
 * Command for displaying a full-size version of a user's avatar.
 */
public class AvatarCommand extends SlashCommand {
	public AvatarCommand() {
		setCommandData(Commands.slash("avatar", "Shows your or someone else's profile picture")
				.addOption(OptionType.USER, "user", "If given, shows the profile picture of the given user", false));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		long userId = event.getOption("user", event.getUser().getIdLong(), OptionMapping::getAsLong);
		event.deferReply().queue();
		event.getJDA().retrieveUserById(userId).queue(
				user -> event.getHook().sendMessageEmbeds(buildAvatarEmbed(event.getGuild(), user)).queue(),
				err -> Responses.error(event.getHook(), String.format("Could not retrieve user with id: `%s`", userId)).queue()
		);
	}

	private MessageEmbed buildAvatarEmbed(@Nullable Guild guild, User createdBy) {
		return new EmbedBuilder()
				.setColor(guild == null ? null : Bot.config.get(guild).getSlashCommand().getDefaultColor())
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Avatar")
				.setImage(createdBy.getEffectiveAvatarUrl() + "?size=4096")
				.build();
	}

}
