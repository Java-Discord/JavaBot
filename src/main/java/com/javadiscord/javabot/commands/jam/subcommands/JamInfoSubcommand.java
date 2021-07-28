package com.javadiscord.javabot.commands.jam.subcommands;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.commands.jam.JamDataManager;
import com.javadiscord.javabot.commands.jam.model.Jam;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class JamInfoSubcommand implements SlashCommandHandler {
	private final JamDataManager dataManager;

	@Override
	public void handle(SlashCommandEvent event) {
		event.deferReply().queue();

		Jam jam;
		OptionMapping idOption = event.getOption("id");
		if (idOption == null) {
			if (event.getGuild() == null) {
				event.getHook().setEphemeral(true);
				event.getHook().sendMessage("Cannot find active Jam without Guild context.").queue();
				return;
			}
			jam = this.dataManager.getActiveJam(event.getGuild().getIdLong());
		} else {
			jam = this.dataManager.getJam(idOption.getAsLong());
		}

		if (jam == null) {
			event.getHook().setEphemeral(true);
			event.getHook().sendMessage("No Jam was found.").queue();
			return;
		}

		User startedByUser = event.getJDA().getUserById(jam.getStartedBy());

		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Jam Information")
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")))
				.addField("Id", Long.toString(jam.getId()), false)
				.addField("Created at", jam.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy 'at' kk:mm:ss 'UTC'")), false)
				.addField("Started by", startedByUser.getAsTag(), false)
				.addField("Starts at", jam.getStartsAt().format(DateTimeFormatter.ofPattern("d MMMM yyyy")), false)
				.addField("Current phase", jam.getCurrentPhase(), false);

		event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
	}
}
