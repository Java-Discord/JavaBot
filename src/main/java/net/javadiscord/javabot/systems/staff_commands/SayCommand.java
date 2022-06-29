package net.javadiscord.javabot.systems.staff_commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * <h3>This class represents the /say command.</h3>
 * This Command lets staff-members let the bot say whatever they want.
 */
@Slf4j
public class SayCommand extends SlashCommand {
	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 */
	public SayCommand() {
		setSlashCommandData(Commands.slash("say", "Let the bot say everything you want!")
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setGuildOnly(true)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping textMapping = event.getOption("text");
		if (textMapping == null) {
			Responses.error(event, "Missing required arguments.").queue();
			return;
		}
		String text = textMapping.getAsString();
		log.info("Posted \"{}\" in \"#{}\" as requested by \"{}\"", text, event.getChannel().getName(), event.getUser().getAsTag());
		event.deferReply(true).queue();
		// TODO: Replace with Webhook
		event.getChannel().sendMessage(text)
				.allowedMentions(Set.of(Message.MentionType.EMOJI))
				.queue(
						m -> event.getHook().sendMessage("Done! " + m.getJumpUrl()).queue(),
						err -> event.getHook().sendMessage("An error occurred. Please try again.").queue()
				);
	}
}