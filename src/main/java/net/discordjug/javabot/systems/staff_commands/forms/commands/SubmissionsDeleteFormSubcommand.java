package net.discordjug.javabot.systems.staff_commands.forms.commands;

import java.util.Optional;

import net.discordjug.javabot.data.config.BotConfig;
import net.discordjug.javabot.systems.staff_commands.forms.dao.FormsRepository;
import net.discordjug.javabot.systems.staff_commands.forms.model.FormData;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import xyz.dynxsty.dih4jda.interactions.AutoCompletable;

/**
 * The `/form submissions-delete` command. Deletes all submission records from a
 * given user from the database. For one-time forms this will allow a user who
 * already submitted the form to submit it again.
 * 
 * @see FormData
 */
public class SubmissionsDeleteFormSubcommand extends FormSubcommand implements AutoCompletable {

	private static final String FORM_USER_FIELD = "user";
	private final FormsRepository formsRepo;

	/**
	 * The main constructor of this subcommand.
	 *
	 * @param formsRepo the forms repository
	 * @param botConfig bot configuration
	 */
	public SubmissionsDeleteFormSubcommand(FormsRepository formsRepo, BotConfig botConfig) {
		super(botConfig, formsRepo);
		this.formsRepo = formsRepo;
		setCommandData(new SubcommandData("submissions-delete", "Deletes submissions of a user in the form").addOptions(
				new OptionData(OptionType.INTEGER, FORM_ID_FIELD, "The ID of a form to delete submissions from", true, true),
				new OptionData(OptionType.USER, FORM_USER_FIELD, "User to delete submissions of", true)));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (!checkForStaffRole(event)) return;
		event.deferReply().setEphemeral(true).queue();
		Optional<FormData> formOpt = formsRepo.getForm(event.getOption(FORM_ID_FIELD, OptionMapping::getAsLong));
		if (formOpt.isEmpty()) {
			event.getHook().sendMessage("Couldn't find a form with this id").queue();
			return;
		}

		User user = event.getOption(FORM_USER_FIELD, OptionMapping::getAsUser);
		FormData form = formOpt.get();

		int count = formsRepo.deleteSubmissions(form, user);
		event.getHook().sendMessage("Deleted " + count + " of this user's submissions!").queue();
	}

	@Override
	public void handleAutoComplete(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery target) {
		handleFormIDAutocomplete(event, target);
	}
}
