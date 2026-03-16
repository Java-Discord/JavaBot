package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * The `/form create` command. This command creates a new, empty form. Newly
 * created forms have no fields, and thus can't be attached (See
 * {@link AttachFormSubcommand}) to messages. Use {@link AddFieldFormSubcommand}
 * to add new fields to the form.
 * 
 * @see FormData
 */
public class CreateFormSubcommand extends FormSubcommand {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public CreateFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("create", "Create a new form").addOptions(
				new OptionData(OptionType.STRING, FORM_TITLE_FIELD, "Form title (shown in modal)", true),
				new OptionData(OptionType.CHANNEL, FORM_SUBMIT_CHANNEL_FIELD, "Channel to log form submissions in",
						true),
				new OptionData(OptionType.STRING, FORM_SUBMIT_MESSAGE_FIELD,
						"Message displayed to the user once they submit the form"),
				new OptionData(OptionType.STRING, FORM_EXPIRATION_FIELD,
						"UTC time after which the form will not accept further submissions. "
								+ FormInteractionManager.DATE_FORMAT_STRING),
				new OptionData(OptionType.BOOLEAN, FORM_ONETIME_FIELD,
						"If the form should only accept one submission per user. Defaults to false.")));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply().setEphemeral(true).queue();
		Optional<Instant> expirationOpt;
		try {
			expirationOpt = FormInteractionManager.parseExpiration(event);
		} catch (IllegalArgumentException e) {
			event.getHook().sendMessage(e.getMessage()).queue();
			return;
		}

		Instant expiration = expirationOpt.orElse(null);

		FormData form = new FormData(0, List.of(), event.getOption(FORM_TITLE_FIELD, OptionMapping::getAsString),
				event.getOption(FORM_SUBMIT_CHANNEL_FIELD, OptionMapping::getAsChannel).getIdLong(),
				event.getOption(FORM_SUBMIT_MESSAGE_FIELD, null, OptionMapping::getAsString), null, null, expiration,
				false, event.getOption(FORM_ONETIME_FIELD, false, OptionMapping::getAsBoolean));

		formsRepo.insertForm(form);
		event.getHook()
				.sendMessage("The form was created! Remember to add fields to it before attaching it to a message.")
				.queue();
	}
}
