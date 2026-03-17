package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.FormInteractionManager;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.discordjug.javabot.util.Responses;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form reopen` command. Reopens a closed form, allowing new submissions.
 * 
 * @see CloseFormSubcommandr
 * @see FormData
 */
public class ReopenFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;
	private final FormInteractionManager interactionManager;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo          the forms repository
	 * @param interactionManager form interaction manager
	 * @param botConfig          main bot configuration
	 */
	public ReopenFormSubcommand(FormsRepository formsRepo, FormInteractionManager interactionManager,
			BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		this.interactionManager = interactionManager;
		setCommandData(new SubcommandData("reopen", "Reopen a closed form. This will allow new submissions.")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a closed form to reopen", true,
						true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		long id = event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong);
		Optional<FormData> formOpt = formsRepo.getForm(id);
		if (formOpt.isEmpty()) {
			Responses.error(event, "A form with this ID was not found.").queue();
			return;
		}
		FormData form = formOpt.get();

		if (!form.closed()) {
			Responses.error(event, "This form is already opened").queue();
			return;
		}

		event.deferReply(true).queue();

		interactionManager.reopenForm(event.getGuild(), form);

		event.getHook().sendMessage("Form reopened!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
