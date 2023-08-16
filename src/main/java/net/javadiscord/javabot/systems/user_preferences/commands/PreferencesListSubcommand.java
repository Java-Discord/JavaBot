package net.javadiscord.javabot.systems.user_preferences.commands;

import net.javadiscord.javabot.util.UserUtils;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.javadiscord.javabot.systems.user_preferences.UserPreferenceService;
import net.javadiscord.javabot.systems.user_preferences.model.Preference;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

/**
 * <h3>This class represents the /preferences list command.</h3>
 */
public class PreferencesListSubcommand extends SlashCommand.Subcommand {
	private final UserPreferenceService service;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param service The {@link UserPreferenceService}
	 */
	public PreferencesListSubcommand(UserPreferenceService service) {
		this.service = service;
		setCommandData(new SubcommandData("list", "Shows all your preferences."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		event.replyEmbeds(buildPreferencesEmbed(service, event.getUser()))
				.setEphemeral(true)
				.queue();
	}

	private @NotNull MessageEmbed buildPreferencesEmbed(UserPreferenceService service, @NotNull User user) {
		EmbedBuilder builder = new EmbedBuilder()
				.setAuthor(UserUtils.getUserTag(user), null, user.getEffectiveAvatarUrl())
				.setTitle(user.getName() + "'s Preferences")
				.setColor(Responses.Type.INFO.getColor());
		for (Preference p : Preference.values()) {
			builder.addField(buildPreferenceField(user, service, p));
		}
		return builder.build();
	}

	private MessageEmbed.@NotNull Field buildPreferenceField(@NotNull User user, @NotNull UserPreferenceService service, Preference preference) {
		String state = service.getOrCreate(user.getIdLong(), preference).getState();
		return new MessageEmbed.Field(preference.toString(), state.isEmpty() ? String.format("`%s` has not yet been set.", preference) : state, true);
	}
}
