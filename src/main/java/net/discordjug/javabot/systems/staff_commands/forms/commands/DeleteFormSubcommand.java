package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form delete` command. Deletes an existing form, also detaching it from
 * a message if it's attached at the time of running this command. This command
 * does NOT delete submission records from the database, see
 * {@link SubmissionsDeleteFormSubcommand}.
 * 
 * @see FormData
 */
public class DeleteFormSubcommand extends FormSubcommand implements AutoCompletable {

	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig
	 * @param botConfig bot configuration
	 */
	public DeleteFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("delete", "Delete an existing form")
				.addOptions(new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a form to delete", true, true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		long id = event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong);
		Optional<FormData> formOpt = formsRepo.getForm(id);
		if (formOpt.isEmpty()) {
			event.reply("A form with this ID was not found.").setEphemeral(true).queue();
			return;
		}

		event.deferReply(true).queue();

		FormData form = formOpt.get();
		formsRepo.deleteForm(form);

		if (form.isAttached()) {
			DetachFormSubcommand.detachFromMessage(form, event.getGuild());
			// TODO send a warning
		}

		event.getHook().sendMessage("Form deleted!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
