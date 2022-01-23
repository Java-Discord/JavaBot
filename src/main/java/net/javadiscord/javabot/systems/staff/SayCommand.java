package net.javadiscord.javabot.systems.staff;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.SlashCommandHandler;

@Slf4j
public class SayCommand implements SlashCommandHandler {

	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		String text = event.getOption("text").getAsString();

		log.info("Posted \"{}\" in \"#{}\" as requested by \"{}\"", text, event.getChannel().getName(), event.getUser().getAsTag());
		event.getChannel().sendMessage(text).queue();
		return event.reply("Done!").setEphemeral(true);
	}
}