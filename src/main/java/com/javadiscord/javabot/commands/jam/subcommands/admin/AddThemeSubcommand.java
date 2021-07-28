package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.commands.jam.JamDataManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public class AddThemeSubcommand extends ActiveJamSubcommand {
	public AddThemeSubcommand(JamDataManager dataManager) {
		super(dataManager, true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam) throws Exception {
		OptionMapping nameOption = event.getOption("name");
		OptionMapping descriptionOption = event.getOption("description");
		if (nameOption == null || descriptionOption == null) {
			event.getHook().sendMessage("Invalid options.").queue();
			return;
		}

		// First check that we don't have too many themes, and make sure none of them have the same name.
		List<JamTheme> themes = this.dataManager.getThemes(activeJam);
		if (themes.size() >= 9) {
			event.getHook().sendMessage("Cannot have more than 9 themes. Remove some if you want to add new ones.").queue();
			return;
		}

		JamTheme theme = new JamTheme();
		theme.setName(nameOption.getAsString());
		theme.setDescription(descriptionOption.getAsString());

		for (JamTheme existingTheme : themes) {
			if (existingTheme.getName().equals(theme.getName())) {
				event.getHook().sendMessage("There is already a theme with that name.").queue();
				return;
			}
		}

		this.dataManager.addTheme(activeJam, theme);
		event.getHook().sendMessage("Added theme **" + theme.getName() + "** to the jam.").queue();
	}
}
