package net.discordjug.javabot.systems.staff_commands;

import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.util.Checks;
import net.discordjug.javabot.util.Responses;
import net.discordjug.javabot.util.UserUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * <h3>This class represents the /say command.</h3>
 * This Command lets staff-members let the bot say whatever they want.
 */
@Slf4j
public class SayCommand extends SlashCommand {
	private final BotConfig botConfig;

	/**
	 * The constructor of this class, which sets the corresponding {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData}.
	 * @param botConfig The injected {@link BotConfig}
	 */
	public SayCommand(BotConfig botConfig) {
		this.botConfig = botConfig;
		setCommandData(Commands.slash("say", "Let the bot say everything you want!")
				.addOption(OptionType.STRING, "text", "The text that should be mirrored.", true)
				.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
				.setContexts(InteractionContextType.GUILD)
		);
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		if (!Checks.hasAdminRole(botConfig, event.getMember())) {
			Responses.replyAdminOnly(event, botConfig.get(event.getGuild())).queue();
			return;
		}
		OptionMapping textMapping = event.getOption("text");
		if (textMapping == null) {
			Responses.replyMissingArguments(event).queue();
			return;
		}
		String text = textMapping.getAsString();
		log.info("Posted \"{}\" in \"#{}\" as requested by \"{}\"", text, event.getChannel().getName(), UserUtils.getUserTag(event.getUser()));
		event.deferReply(true).queue();
		event.getChannel().sendMessage(text)
				.setAllowedMentions(Set.of(Message.MentionType.EMOJI, Message.MentionType.CHANNEL))
				.queue(m -> event.getHook().sendMessage("Done! " + m.getJumpUrl()).queue(),
						err -> event.getHook().sendMessage("An error occurred. Please try again.").queue()
				);
	}
}