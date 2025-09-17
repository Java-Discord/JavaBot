package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.List;
import java.util.Optional;

import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.commands.application.SlashCommand.Subcommand;

/**
 * The `/form create` command.
 */
public class CreateFormSubcommand extends Subcommand {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 */
	public CreateFormSubcommand(FormsRepository formsRepo) {
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("create", "Create a new form").addOptions(
				new OptionData(OptionType.STRING, "title", "Form title (shown in modal)", true),
				new OptionData(OptionType.CHANNEL, "submit-channel", "Channel to log form submissions in", true),
				new OptionData(OptionType.STRING, "submit-message",
						"Message displayed to the user once they submit the form"),
				new OptionData(OptionType.STRING, "expiration",
						"UTC time after which the form will not accept further submissions. "
								+ FormInteractionManager.DATE_FORMAT_STRING),
				new OptionData(OptionType.BOOLEAN, "onetime",
						"If the form should only accept one submission per user. Defaults to false.")));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {

		event.deferReply().setEphemeral(true).queue();
		String expirationStr = event.getOption("expiration", null, OptionMapping::getAsString);
		Optional<Long> expirationOpt = FormInteractionManager.parseExpiration(event);

		if (expirationOpt.isEmpty()) return;

		long expiration = expirationOpt.get();

		long formId = System.currentTimeMillis();
		FormData form = new FormData(formId, List.of(), event.getOption("title", OptionMapping::getAsString),
				event.getOption("submit-channel", OptionMapping::getAsChannel).getIdLong(),
				event.getOption("submit-message", null, OptionMapping::getAsString), null, null, expiration, false,
				event.getOption("onetime", false, OptionMapping::getAsBoolean));

		formsRepo.insertForm(form);
		event.getHook()
				.sendMessage("The form was created! Remember to add fields to it before attaching it to a message.")
				.queue();
	}
}
