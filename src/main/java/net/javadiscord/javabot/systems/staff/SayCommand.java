package net.javadiscord.javabot.systems.staff;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.SlashCommandHandler;

/**
 * Command that lets staff-members let the bot say whatever they want.
 */
@Slf4j
public class SayCommand implements SlashCommandHandler {
	@Override
	public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
		var textOption = event.getOption("text");
		if (textOption == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		var text = textOption.getAsString();
		log.info("Posted \"{}\" in \"#{}\" as requested by \"{}\"", text, event.getChannel().getName(), event.getUser().getAsTag());
		event.getChannel().sendMessage(text).queue();
		return event.reply("Done!").setEphemeral(true);
	}
}