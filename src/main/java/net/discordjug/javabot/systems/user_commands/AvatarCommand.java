package net.discordjug.javabot.systems.user_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;

/**
 * This class represents the `/avatar` command.
 */
public class AvatarCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public AvatarCommand() {
		setCommandData(Commands.slash("avatar", "Shows your or someone else's profile picture")
				.addOption(OptionType.USER, "user", "If given, shows the profile picture of the given user", false)
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		long userId = event.getOption("user", event.getUser().getIdLong(), OptionMapping::getAsLong);
		event.deferReply().queue();
		event.getJDA().retrieveUserById(userId).queue(
				user -> event.getHook().sendMessageEmbeds(buildAvatarEmbed(user)).queue(),
				err -> Responses.error(event.getHook(), "Could not retrieve user with id: `%s`", userId).queue()
		);
	}

	private @NotNull MessageEmbed buildAvatarEmbed(@NotNull User createdBy) {
		return new EmbedBuilder()
				.setColor(Responses.Type.DEFAULT.getColor())
				.setAuthor(UserUtils.getUserTag(createdBy), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Avatar")
				.setImage(createdBy.getEffectiveAvatarUrl() + "?size=4096")
				.build();
	}

}
